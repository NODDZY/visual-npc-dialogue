package com.npcdialogue;

import com.google.inject.Provides;
import com.npcdialogue.model.Dialogue;
import com.npcdialogue.service.ChatboxService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
	name = "AudioVisual NPC Dialogue"
)
public class DialoguePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private DialogueConfig config;

	@Inject
	private ChatboxService chatboxService;

	// The last NPC dialogue
	private Dialogue lastNpcDialogue = null;

	// The last player dialogue
	private Dialogue lastPlayerDialogue = null;

	@Provides
	DialogueConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DialogueConfig.class);
	}

	/**
	 * Check for dialogue every tick
	 */
	@Subscribe
	public void onGameTick(GameTick event)
	{
		// If the dialogue interface is displayed
		if (checkWidgetDialogue())
		{
			// If chatbox dialogue is set in config send dialogue in chat
			if (lastNpcDialogue != null && config.displayChatboxNpcDialogue()) {
				chatboxService.addDialogMessage(lastNpcDialogue);
			}
			if (lastPlayerDialogue != null && config.displayChatboxPlayerDialogue()) {
				chatboxService.addDialogMessage(lastPlayerDialogue);
			}
		}
	}

	/**
	 * Checks if dialogue interface widget is present and valid (containing a new dialogue message)
     */
	private boolean checkWidgetDialogue() {
		// Get message from dialogue interface widget
		final Dialogue npcDialogue = getDialogueMessage(WidgetInfo.DIALOG_NPC_TEXT.getGroupId(), WidgetInfo.DIALOG_NPC_NAME.getChildId(), WidgetInfo.DIALOG_NPC_TEXT.getChildId());
		final Dialogue playerDialogue = getDialogueMessage(WidgetID.DIALOG_PLAYER_GROUP_ID, WidgetInfo.DIALOG_NPC_NAME.getChildId(), WidgetInfo.DIALOG_NPC_TEXT.getChildId());

		// Check if NPC has dialogue interface
		// Check that dialogue box is valid and that it is not a duplicate
		if (npcDialogue.getContent() != null && (lastNpcDialogue == null || !lastNpcDialogue.getContent().equals(npcDialogue.getContent())))
		{
			log.debug("NPC Dialogue: " + npcDialogue);
			lastNpcDialogue = npcDialogue;
			lastPlayerDialogue = null;
			return true;
		}

		// Check if player has dialogue interface
		// Check that dialogue box is valid and that it is not a duplicate
		if (playerDialogue.getContent() != null && (lastPlayerDialogue == null || !lastPlayerDialogue.getContent().equals(playerDialogue.getContent())))
		{
			log.debug("Player Dialogue: " + playerDialogue);
			lastPlayerDialogue = playerDialogue;
			lastNpcDialogue = null;
			return true;
		}

		// Return false if there is no dialogue interface present
		return false;
	}

	/**
	 * Gets {@link Dialogue} from dialogue interface widget
	 *
	 * @param group     The group id for the dialogue widget
	 * @param nameChild The child id of the name in the dialogue widget
	 * @param textChild The child id of the content in the dialogue widget
	 * @return The sanitized dialogue from the dialogue widget
	 */
	private Dialogue getDialogueMessage(final int group, final int nameChild, final int textChild)
	{
		return new Dialogue(client.getWidget(group, nameChild) == null ? null : Text.sanitizeMultilineText(client.getWidget(group, nameChild).getText()),
				client.getWidget(group, textChild) == null ? null : Text.sanitizeMultilineText(client.getWidget(group, textChild).getText()));
	}
}
