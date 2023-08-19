package com.npcdialogue;

import com.google.inject.Provides;
import com.npcdialogue.model.Dialogue;
import com.npcdialogue.service.ChatboxService;
import com.npcdialogue.service.OverheadService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
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
	name = "AudioVisual NPC Dialogue"
)
public class DialoguePlugin extends Plugin {
	@Inject
	private Client client;

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
	 * Check for dialogue every tick
	 * Also check overhead text duration
	 */
	@Subscribe
	public void onGameTick(GameTick event) {
		// If the dialogue interface is displayed
		if (getWidgetDialogue()) {
			// If chatbox dialogue is set in config send dialogue in chat
			if (lastNpcDialogue != null && config.displayChatboxNpcDialogue()) {
				chatboxService.addDialogMessage(lastNpcDialogue);
			}
			if (lastPlayerDialogue != null && config.displayChatboxPlayerDialogue()) {
				chatboxService.addDialogMessage(lastPlayerDialogue);
			}

			// If overhead dialogue is set in config set overhead dialogue
			// Also add actor/timestamp to lastActorOverheadTickTime map
			if (lastNpcDialogue != null && config.displayOverheadNpcDialogue()) {
				if (lastInteractedActor.getName() == null || !lastInteractedActor.getName().equals(lastNpcDialogue.getName())) {
					Actor npc = findActor();
					overheadService.setOverheadTextNpc(npc, lastNpcDialogue);
					lastActorOverheadTickTime.put(npc, client.getTickCount());
				} else {
					overheadService.setOverheadTextNpc(lastInteractedActor, lastNpcDialogue);
					lastActorOverheadTickTime.put(lastInteractedActor, client.getTickCount());
				}
			}
			if (lastPlayerDialogue != null && config.displayOverheadPlayerDialogue()) {
				overheadService.setOverheadTextPlayer(lastPlayerDialogue);
				lastActorOverheadTickTime.put(client.getLocalPlayer(), client.getTickCount());
			}
		}

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
	 * Checks if dialogue interface widget is present and valid (containing a new dialogue message)
     */
	private boolean getWidgetDialogue() {
		// Get message from dialogue interface widget
		final Dialogue npcDialogue = getDialogueMessage(WidgetInfo.DIALOG_NPC_TEXT.getGroupId(), WidgetInfo.DIALOG_NPC_NAME.getChildId(), WidgetInfo.DIALOG_NPC_TEXT.getChildId());
		final Dialogue playerDialogue = getDialogueMessage(WidgetID.DIALOG_PLAYER_GROUP_ID, WidgetInfo.DIALOG_NPC_NAME.getChildId(), WidgetInfo.DIALOG_NPC_TEXT.getChildId());

		// Check if NPC has dialogue interface
		// Check that dialogue box is valid and that it is not a duplicate
		if (npcDialogue.getContent() != null && (lastNpcDialogue == null || !lastNpcDialogue.getContent().equals(npcDialogue.getContent())) && !ignoredActor(npcDialogue.getName())) {
			log.debug("NPC Dialogue: " + npcDialogue);
			lastNpcDialogue = npcDialogue;
			lastPlayerDialogue = null;
			return true;
		}

		// Check if player has dialogue interface
		// Check that dialogue box is valid and that it is not a duplicate
		if (playerDialogue.getContent() != null && (lastPlayerDialogue == null || !lastPlayerDialogue.getContent().equals(playerDialogue.getContent()))) {
			log.debug("Player Dialogue: " + playerDialogue);
			lastPlayerDialogue = playerDialogue;
			lastNpcDialogue = null;
			return true;
		}

		// Return false if there is no dialogue interface present
		return false;
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

		// Return the found actor if found
		// If not found return the last interacted with NPC
		if (actor != null) {
			log.debug("Found matching actor: " + actor.getName() + " " + actor.getId());
			return actor;
		} else {
			log.warn("Unable to find matching actor. Fallback to using latest NPC: " + lastInteractedActor.getName());
			return lastInteractedActor;
		}
	}

	/**
	 * Gets {@link Dialogue} from dialogue interface widget
	 *
	 * @param group     The group id for the dialogue widget
	 * @param nameChild The child id of the name in the dialogue widget
	 * @param textChild The child id of the content in the dialogue widget
	 * @return The sanitized dialogue from the dialogue widget
	 */
	private Dialogue getDialogueMessage(final int group, final int nameChild, final int textChild) {
		Widget nameWidget = client.getWidget(group, nameChild);
		Widget textWidget = client.getWidget(group, textChild);

		String sanitizedName = (nameWidget == null) ? null : Text.sanitizeMultilineText(nameWidget.getText());
		String sanitizedText = (textWidget == null) ? null : Text.sanitizeMultilineText(textWidget.getText());

		return new Dialogue(sanitizedName, sanitizedText);
	}

	/**
	 * Check if actor is listed in the NPC ignore list
	 *
	 * @param name The NPC name to check
	 * @return Whether actor is in the ignore list
	 */
	private boolean ignoredActor(String name) {
		if (name == null || config.ignoredNPCs() == null) {
			return false;
		}

		String[] names = config.ignoredNPCs().split(",");
		for (String n : names) {
			if (n.trim().equals(name) || chatboxService.trimName(n).equals(name)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Get the last NPC player entered dialogue with
	 */
	@Subscribe
	private void onInteractingChanged(InteractingChanged event) {
		if (event.getTarget() == null || event.getSource() != client.getLocalPlayer()) {
			return;
		}
		lastInteractedActor = event.getTarget();
		log.info("Interacted with actor: " + lastInteractedActor.getName());
	}

	/**
	 * Reset and clear overhead text and actor history when logging in/out
	 */
	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged) {
		switch (gameStateChanged.getGameState()) {
			case LOGGED_IN:
				// When logged in clear all overhead text from previous NPCs
				for (Actor actor : lastActorOverheadTickTime.keySet()) {
					overheadService.clearOverheadText(actor);
				}
				break;
			case CONNECTION_LOST:
			case HOPPING:
			case LOGIN_SCREEN:
				// Clear actor history when logging out, hopping or losing connection
				lastActorOverheadTickTime.clear();
				break;
			default:
				break;
		}
	}
}
