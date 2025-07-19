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
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Slf4j
@PluginDescriptor(
    name = "Visual NPC Dialogue",
    description = "Adds dialogue to the chatbox and above NPCs heads",
    tags = { "npc", "dialogue", "chat" }
)
public class DialoguePlugin extends Plugin {
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private DialogueConfig config;
    @Inject private ChatboxService chatboxService;
    @Inject private OverheadService overheadService;

    private Dialogue lastNpcDialogue = null;
    private Dialogue lastPlayerDialogue = null;
    private Actor lastInteractedActor = null;
    // Collection of actors and their overhead text timestamp
    private final Map<Actor, OverheadEntry> overheadEntries = new HashMap<>();

    /**
     * Represents an overhead text entry with a start tick and duration.
     * Used to determine when overhead text should expire and be removed.
     */
    private static class OverheadEntry {
        final int startTick;
        final int durationTicks;

        OverheadEntry(int startTick, int durationTicks) {
            this.startTick = startTick;
            this.durationTicks = durationTicks;
        }
    }

    @Provides
    DialogueConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(DialogueConfig.class);
    }

    /**
     * When a widget loads check if it is a dialogue widget and process the dialogue
     */
    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        // Check if widget is DIALOG_NPC
        if(event.getGroupId() == InterfaceID.DIALOG_NPC) {
            Widget widget = client.getWidget(ComponentID.DIALOG_NPC_TEXT);
            clientThread.invokeLater(() -> {
                String name = Text.sanitizeMultilineText(client.getWidget(ComponentID.DIALOG_NPC_NAME).getText());
                String message = Text.sanitizeMultilineText(widget.getText());

                Dialogue dialogue = new Dialogue(name, message);
                log.debug("NPC dialogue registered: {}", dialogue);

                processDialogue(dialogue);
            });
        }
        // Check if widget is DIALOG_PLAYER
        else if(event.getGroupId() == InterfaceID.DIALOG_PLAYER) {
            Widget widget = client.getWidget(ComponentID.DIALOG_PLAYER_TEXT);
            clientThread.invokeLater(() -> {
                String name = Text.sanitizeMultilineText(client.getLocalPlayer().getName());
                String message = Text.sanitizeMultilineText(widget.getText());

                Dialogue dialogue = new Dialogue(name, message);
                log.debug("Player dialogue registered: {}", dialogue);

                processDialogue(dialogue);
            });
        }
    }

    /**
     * Looks at current dialogue and stores it if valid
     * @param dialogue Dialogue to process and display
     */
    private void processDialogue(Dialogue dialogue) {
        // Check if player has dialogue interface
        if (dialogue.getName().equals(client.getLocalPlayer().getName()) && dialogue.getText() != null) {
            lastPlayerDialogue = dialogue;
            lastNpcDialogue = null;
            displayDialoguePlayer();
        }
        // Check if NPC has dialogue interface
        else if (dialogue.getText() != null && !DialogueUtils.isIgnoredActor(config.ignoredNPCs(), dialogue.getName())) {
            lastNpcDialogue = dialogue;
            lastPlayerDialogue = null;
            displayDialogueNPC();
        }
    }

    /**
     * Calculates the total display time in game ticks and updates the overheadEntries map.
     * @param actor The actor (NPC or player) for which the dialogue is displayed.
     * @param dialogue The dialogue text being shown.
     */
    private void setOverheadWithDuration(Actor actor, Dialogue dialogue) {
        int baseTicks = 3;
        int extraPerCharMs = config.extraDisplayTimePerCharacter();
        int messageLength = dialogue.getText().length();
        int totalMs = messageLength * extraPerCharMs;

        // Convert milliseconds to ticks (600 ms per tick), rounding up
        int totalTicks = Math.max(baseTicks, (int) Math.ceil((double) totalMs / 600.0));

        overheadEntries.put(actor, new OverheadEntry(client.getTickCount(), totalTicks));
        log.debug("Overhead for [{}]: len={}, totalMs={}, totalTicks={}",
            actor.getName(), messageLength, totalMs, totalTicks);

    }


    /**
     * Displays player dialogue overhead or in chatbox based on configuration
     * Calculates overhead display duration dynamically based on message length
     */
    private void displayDialoguePlayer() {
        // Chatbox dialogue
        if (lastPlayerDialogue != null && config.displayChatboxPlayerDialogue()) {
            chatboxService.addDialogMessage(lastPlayerDialogue);
        }
        // Overhead dialogue
        if (lastPlayerDialogue != null && config.displayOverheadPlayerDialogue()) {
            overheadService.setOverheadTextPlayer(lastPlayerDialogue);
            setOverheadWithDuration(client.getLocalPlayer(), lastPlayerDialogue);
        }
    }

    /**
     * Displays NPC dialogue overhead or in chatbox.
     * Overhead timing is centralized via setOverheadWithDuration().
     */
    private void displayDialogueNPC()
    {
        if (lastNpcDialogue != null && config.displayChatboxNpcDialogue())
        {
            chatboxService.addDialogMessage(lastNpcDialogue);
        }

        if (lastNpcDialogue != null && config.displayOverheadNpcDialogue())
        {
            Actor npc = findActor();
            overheadService.setOverheadTextNpc(npc, lastNpcDialogue);
            setOverheadWithDuration(npc, lastNpcDialogue);
        }
    }

    /**
     * Finds actor based on NPC name. To be used when dialogue is not started by interaction
     * @return The found actor or the last interacted actor
     */
    private Actor findActor() {
        NPC actor = null;
        for (NPC npc : client.getNpcs()) {
            // Check the NPC cache for actor based on NPC name
            if (npc.getName() != null && Text.sanitizeMultilineText(npc.getName()).equals(lastNpcDialogue.getName())) {
                actor = npc;
                break;
            }
        }

        if (actor != null) {
            // Return the found actor if found
            log.debug("Found matching actor: [{}] {}", actor.getId(), actor.getName());
            return actor;
        } else {
            // Return the last interacted with NPC if not found
            log.warn("Unable to find matching actor. Fallback to using latest NPC: {}", lastInteractedActor.getName());
            return lastInteractedActor;
        }
    }

    /**
     * Checks overhead text duration and clears overhead text if expired
     */
    @Subscribe
    public void onGameTick(GameTick event) {
        Iterator<Map.Entry<Actor, OverheadEntry>> iterator = overheadEntries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Actor, OverheadEntry> entry = iterator.next();
            Actor actor = entry.getKey();
            OverheadEntry overhead = entry.getValue();

            if (client.getTickCount() - overhead.startTick > overhead.durationTicks) {
                overheadService.clearOverheadText(actor);
                iterator.remove();
            }
        }
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
                if (overheadEntries.remove(client.getLocalPlayer()) != null) {
                    log.debug("Player sent chat while overhead was being displayed. Cleared overhead counter for player actor");
                }
            }
        }
    }

    /**
     * Save NPCs the player interacts with to lastInteractedActor variable
     */
    @Subscribe
    private void onInteractingChanged(InteractingChanged event) {
        if (event.getTarget() == null || event.getSource() != client.getLocalPlayer()) {
            return;
        }
        lastInteractedActor = event.getTarget();
        log.debug("Interacted with actor: {}", lastInteractedActor.getName());
    }

    /**
     * Reset and clear overhead text and actor history when logging in/out
     */
    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        switch (gameStateChanged.getGameState()) {
            case LOGGED_IN:
                // When logging in clear all known overhead text
                for (Actor actor : overheadEntries.keySet()) {
                    overheadService.clearOverheadText(actor);
                }
                break;
            case CONNECTION_LOST:
            case HOPPING:
            case LOGIN_SCREEN:
                // Clear actor history when logging out, hopping or losing connection
                log.debug("Clearing actor history...");
                overheadEntries.clear();
                break;
            default:
                break;
        }
    }
}
