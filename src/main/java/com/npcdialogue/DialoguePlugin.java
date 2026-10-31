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
                NPC actor = findActor(dialogue.getName());
                if (actor == null) {
                    log.debug("No valid actor found for NPC overhead dialogue, skipping overhead");
                    return;
                }
                lastInteractedActor = actor;
                overheadService.setOverheadTextNpc(actor, dialogue);
            }
        }
    }

    /**
     * Finds NPC based on name, interaction and stored Actor
     */
    private NPC findActor(String name) {
        Actor interacting = client.getLocalPlayer() == null ? null : client.getLocalPlayer().getInteracting();
        if (interacting instanceof NPC) {
            NPC interactingNpc = (NPC) interacting;
            if (name.equals(interactingNpc.getName()) && interactingNpc.getId() != -1) {
                return interactingNpc;
            }
        }

        if (lastInteractedActor != null && lastInteractedActor.getId() != -1 && name.equals(lastInteractedActor.getName())) {
            return lastInteractedActor;
        }

        NPC matchingNpc = null;
        for (NPC npc : client.getTopLevelWorldView().npcs()) {
            if (npc.getId() != -1 && name.equals(npc.getName())) {
                if (matchingNpc != null) {
                    log.debug("Multiple NPCs named {} found; skipping overhead dialogue", name);
                    return null;
                }
                matchingNpc = npc;
            }
        }

        return matchingNpc;
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

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        switch (event.getMenuAction()) {
            case WIDGET_TARGET_ON_NPC:
            case NPC_FIRST_OPTION:
            case NPC_SECOND_OPTION:
            case NPC_THIRD_OPTION:
            case NPC_FOURTH_OPTION:
            case NPC_FIFTH_OPTION:
                NPC npc = event.getMenuEntry().getNpc();
                if (npc != null) {
                    lastInteractedActor = npc;
                    log.debug("Selected actor: {} ({})", npc.getName(), npc.getId());
                }
                break;
            default:
                break;
        }
    }

    @Subscribe
    public void onInteractingChanged(InteractingChanged event) {
        if (event.getSource() != client.getLocalPlayer() || !(event.getTarget() instanceof NPC)) {
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
