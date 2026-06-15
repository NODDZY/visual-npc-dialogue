package com.npcdialogue;

import com.google.inject.Provides;
import com.npcdialogue.model.Dialogue;
import com.npcdialogue.service.ChatboxService;
import com.npcdialogue.service.OverheadService;
import com.npcdialogue.util.DialogueUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
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

import static net.runelite.api.gameval.VarbitID.CUTSCENE_STATUS;

@Slf4j
@PluginDescriptor(
    name = "Visual NPC Dialogue",
    description = "Adds dialogue to the chatbox and above NPCs heads",
    tags = { "npc", "dialogue", "chat" }
)
public class DialoguePlugin extends Plugin {
    private static final int SCRIPT_CHAT_DIALOGUE = 600;

    @Inject private Client client;
    @Inject private DialogueConfig config;
    @Inject private ChatboxService chatboxService;
    @Inject private OverheadService overheadService;

    private NPC lastInteractedActor = null;

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
            }
        }

        if (dialogue.getType() == Dialogue.Type.NPC) {
            if (config.displayChatboxNpcDialogue()) {
                chatboxService.addDialogMessage(dialogue);
            }
            if (config.displayOverheadNpcDialogue()) {
                Actor actor = findActor(dialogue.getName());
                if (actor == null) {
                    log.debug("No valid actor found for NPC overhead dialogue, skipping overhead");
                    return;
                }
                overheadService.setOverheadTextNpc(actor, dialogue);
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

        if (lastInteractedActor != null && lastInteractedActor.getId() != -1) {
            log.debug("Unable to find matching actor. Fallback to using lastInteractedActor: {} ({})", lastInteractedActor.getName(), lastInteractedActor.getId());
            return lastInteractedActor;
        }
        return null;
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

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        switch (gameStateChanged.getGameState()) {
            case CONNECTION_LOST:
            case HOPPING:
            case LOGIN_SCREEN:
                lastInteractedActor = null;
                break;
            default:
                break;
        }
    }
}
