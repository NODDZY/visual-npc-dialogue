package com.npcdialogue;

import com.google.inject.Provides;
import com.npcdialogue.model.Dialogue;
import com.npcdialogue.service.ChatboxService;
import com.npcdialogue.service.OverheadService;
import com.npcdialogue.util.DialogueUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static net.runelite.api.gameval.VarbitID.CUTSCENE_STATUS;

@Slf4j
@PluginDescriptor(
    name = "Visual NPC Dialogue",
    description = "Adds dialogue to the chatbox and above NPCs heads",
    tags = { "npc", "dialogue", "chat" }
)
public class DialoguePlugin extends Plugin {
    private static final int SCRIPT_CHAT_DIALOGUE = 600;
    private static final int TIMEOUT_TICKS = 3;

    @Inject private Client client;
    @Inject private DialogueConfig config;
    @Inject private ChatboxService chatboxService;
    @Inject private OverheadService overheadService;

    private NPC lastInteractedActor = null;
    // Collection of actors and their overhead text timestamp
    private final Map<Actor, Integer> lastActorOverheadTickTime = new HashMap<>();

    @Provides
    DialogueConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(DialogueConfig.class);
    }

    /**
     * Looks at current dialogue and stores it if valid
     * @param dialogue Dialogue to process and display
     */
    private void processDialogue(Dialogue dialogue) {
        if (DialogueUtils.isIgnoredActor(config.ignoredNPCs(), dialogue.getName())) {
            return;
        }
        displayDialogue(dialogue);
    }

    private void displayDialogue(Dialogue dialogue) {
        if (dialogue.getType() == Dialogue.Type.PLAYER) {
            if (config.displayChatboxPlayerDialogue()) {
                chatboxService.addDialogMessage(dialogue);
            }
            if (config.displayOverheadPlayerDialogue()) {
                overheadService.setOverheadTextPlayer(dialogue);
                lastActorOverheadTickTime.put(client.getLocalPlayer(), client.getTickCount());
            }
        }

        if (dialogue.getType() == Dialogue.Type.NPC) {
            if (config.displayChatboxNpcDialogue()) {
                chatboxService.addDialogMessage(dialogue);
            }
            if (config.displayOverheadNpcDialogue()) {
                Actor actor = findActor(dialogue.getName());
                if (actor == null) {
                    log.info("No valid actor found for NPC overhead dialogue, skipping overhead");
                    return;
                }
                overheadService.setOverheadTextNpc(actor, dialogue);
                lastActorOverheadTickTime.put(actor, client.getTickCount());
            }
        }
    }

    /**
     * Finds actor based on NPC name. To be used when dialogue is not started by interaction
     * @return The found actor or the last interacted actor
     */
    private Actor findActor(String name) {
        if (lastInteractedActor != null && name.equals(lastInteractedActor.getName())) {
            return lastInteractedActor;
        }

        for (NPC npc : client.getTopLevelWorldView().npcs()) {
            if (name.equals(npc.getName())) {
                return npc;
            }
        }

        if (lastInteractedActor != null) {
            log.debug("Unable to find matching actor. Fallback to using lastInteractedActor: {}", lastInteractedActor.getName());
            return lastInteractedActor;
        }
        return null;
    }

    /**
     * Checks overhead text duration and clears overhead text if expired
     */
    @Subscribe
    public void onGameTick(GameTick event) {
        for (Iterator<Actor> iterator = lastActorOverheadTickTime.keySet().iterator(); iterator.hasNext(); ) {
            Actor actor = iterator.next();
            if (client.getTickCount() - lastActorOverheadTickTime.get(actor) > TIMEOUT_TICKS) {
                overheadService.clearOverheadText(actor);
                iterator.remove();
            }
        }
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event) {
        if (event.getScriptId() != SCRIPT_CHAT_DIALOGUE) return;
        if (config.disableDialogueDuringCutscene() && client.getVarbitValue(CUTSCENE_STATUS) == 1) return;

        Widget text;
        Widget name;

        if (client.getWidget(InterfaceID.ChatLeft.UNIVERSE) != null) {
            // NPC
            text = client.getWidget(InterfaceID.ChatLeft.TEXT);
            name = client.getWidget(InterfaceID.ChatLeft.NAME);
        } else if (client.getWidget(InterfaceID.ChatRight.UNIVERSE) != null) {
            // PLAYER
            text = client.getWidget(InterfaceID.ChatRight.TEXT);
            name = client.getWidget(InterfaceID.ChatRight.NAME);
        } else {
            log.debug("Could not find dialogue widgets post script");
            return;
        }
        if (text == null || name == null) return;

        boolean isPlayer = name.getText().equals(client.getLocalPlayer().getName());

        processDialogue(new Dialogue(name.getText(), Text.sanitizeMultilineText(text.getText()), isPlayer ? Dialogue.Type.PLAYER : Dialogue.Type.NPC));
    }

    /**
     * Looks for player chat messages so overhead is not prematurely cleared
     */
    @Subscribe
    public void onChatMessage(ChatMessage event) {
        // Check if player sends public chat message while player overhead is being shown
        if (client.getLocalPlayer() != null && event.getType() == ChatMessageType.PUBLICCHAT && event.getName().equals(client.getLocalPlayer().getName())) {
            if (client.getLocalPlayer().getOverheadText() != null) {
                // Remove timer to not prematurely clear public chat overhead
                if (lastActorOverheadTickTime.remove(client.getLocalPlayer()) != null) {
                    log.debug("Player sent chat while overhead was being displayed. Cleared overhead counter for player actor");
                }
            }
        }
    }

    /**
     * Save NPCs the player interacts with to lastInteractedActor variable
     */
    @Subscribe
    public void onInteractingChanged(InteractingChanged event) {
        if (event.getTarget() == null || event.getSource() != client.getLocalPlayer()) {
            return;
        }

        lastInteractedActor = (NPC) event.getTarget();
        log.debug("Interacted with actor: {} ({})", lastInteractedActor.getName(), lastInteractedActor.getId());
    }

    /**
     * Reset and clear overhead text and actor history when logging in/out
     */
    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        switch (gameStateChanged.getGameState()) {
            case LOGGED_IN:
                // When logging in clear all known overhead text
                for (Actor actor : lastActorOverheadTickTime.keySet()) {
                    overheadService.clearOverheadText(actor);
                }
                lastActorOverheadTickTime.clear();
                break;
            case CONNECTION_LOST:
            case HOPPING:
            case LOGIN_SCREEN:
                // Clear actor history when logging out, hopping or losing connection
                lastInteractedActor = null;
                lastActorOverheadTickTime.clear();
                break;
            default:
                break;
        }
    }
}
