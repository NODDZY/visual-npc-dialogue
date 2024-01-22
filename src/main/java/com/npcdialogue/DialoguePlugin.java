package com.npcdialogue;

import com.google.inject.Provides;
import com.npcdialogue.model.Dialogue;
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
	name = "Visual NPC Dialogue"
)
public class DialoguePlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private Util util;

	@Inject
	private DialogueConfig config;

	@Inject
	private ChatboxService chatboxService;

	@Inject
	private OverheadService overheadService;

	// The last NPC dialogue
	private Dialogue lastNpcDialogue = null;

	// The last player dialogue
	private Dialogue lastPlayerDialogue = null;

	// The last actor the player entered dialogue with
	private Actor lastInteractedActor = null;

	// HashMap that includes the tick time for the last overhead message for each actor
	private final Map<Actor, Integer> lastActorOverheadTickTime = new HashMap<>();

	@Provides
	DialogueConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(DialogueConfig.class);
	}

	/**
	 * When a widget loads check if it is a dialgue widget and process the dialogue.
	 */
	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event) {
		// Check if widget is NPC dialogue
		if(event.getGroupId() == InterfaceID.DIALOG_NPC ) {
			Widget widget = client.getWidget(ComponentID.DIALOG_NPC_TEXT);
			clientThread.invokeLater(() -> {
				String name = Text.sanitizeMultilineText(client.getWidget(ComponentID.DIALOG_NPC_NAME).getText());
				String message = Text.sanitizeMultilineText(widget.getText());

				Dialogue dialogue = new Dialogue(name, message);
				log.debug("NPC dialogue registered: " + dialogue);

				processDialogue(dialogue);
			});
		}
		// Check if widget is Player dialogue
		else if(event.getGroupId() == InterfaceID.DIALOG_PLAYER) {
			Widget widget = client.getWidget(ComponentID.DIALOG_PLAYER_TEXT);
			clientThread.invokeLater(() -> {
				String name = Text.sanitizeMultilineText(client.getLocalPlayer().getName());
				String message = Text.sanitizeMultilineText(widget.getText());

				Dialogue dialogue = new Dialogue(name, message);
				log.debug("Player dialogue registered: " + dialogue);

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
		else if (dialogue.getText() != null && !util.isIgnoredActor(config.ignoredNPCs(), dialogue.getName())) {
			lastNpcDialogue = dialogue;
			lastPlayerDialogue = null;
			displayDialogueNPC();
		}
	}

	/**
	 * Displays player dialogue overhead or in chatbox based on configuration
	 */
	private void displayDialoguePlayer() {
		// Chatbox dialogue
		if (lastPlayerDialogue != null && config.displayChatboxPlayerDialogue()) {
			chatboxService.addDialogMessage(lastPlayerDialogue);
		}
		// Overhead dialogue
		if (lastPlayerDialogue != null && config.displayOverheadPlayerDialogue()) {
			overheadService.setOverheadTextPlayer(lastPlayerDialogue);
			lastActorOverheadTickTime.put(client.getLocalPlayer(), client.getTickCount());
		}
	}

	/**
	 * Displays NPC dialogue overhead or in chatbox based on configuration
	 */
	private void displayDialogueNPC() {
		// Chatbox dialogue
		if (lastNpcDialogue != null && config.displayChatboxNpcDialogue()) {
			chatboxService.addDialogMessage(lastNpcDialogue);
		}
		// Overhead dialogue
		if (lastNpcDialogue != null && config.displayOverheadNpcDialogue()) {
			// Check if NPC is saved in lastInteractedActor
			if (lastInteractedActor == null || !lastInteractedActor.getName().equals(lastNpcDialogue.getName())) {
				// If not -> find NPC
				Actor npc = findActor();
				overheadService.setOverheadTextNpc(npc, lastNpcDialogue);
				lastActorOverheadTickTime.put(npc, client.getTickCount());
			} else {
				overheadService.setOverheadTextNpc(lastInteractedActor, lastNpcDialogue);
				lastActorOverheadTickTime.put(lastInteractedActor, client.getTickCount());
			}
		}
	}

	/**
	 * Finds actor based on NPC name. To be used when dialogue is not started by interaction.
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
			log.debug("Found matching actor: [" + actor.getId() + "] " + actor.getName());
			return actor;
		} else {
			// Return the last interacted with NPC if not found
			log.warn("Unable to find matching actor. Fallback to using latest NPC: " + lastInteractedActor.getName());
			return lastInteractedActor;
		}
	}

	/**
	 * Checks overhead text duration and clears overhead text if expired
	 */
	@Subscribe
	public void onGameTick(GameTick event) {
		for (Iterator<Actor> iterator = lastActorOverheadTickTime.keySet().iterator(); iterator.hasNext(); ) {
			Actor actor = iterator.next();
			// How long overhead text should last for
			int TIMEOUT_TICKS = 3;
			if (client.getTickCount() - lastActorOverheadTickTime.get(actor) > TIMEOUT_TICKS) {
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
	private void onInteractingChanged(InteractingChanged event) {
		if (event.getTarget() == null || event.getSource() != client.getLocalPlayer()) {
			return;
		}
		lastInteractedActor = event.getTarget();
		log.debug("Interacted with actor: " + lastInteractedActor.getName());
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
				break;
			case CONNECTION_LOST:
			case HOPPING:
			case LOGIN_SCREEN:
				// Clear actor history when logging out, hopping or losing connection
				log.debug("Clearing actor history...");
				lastActorOverheadTickTime.clear();
				break;
			default:
				break;
		}
	}
}
