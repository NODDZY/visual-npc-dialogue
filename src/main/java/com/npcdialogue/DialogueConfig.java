package com.npcdialogue;

import net.runelite.client.config.*;

import java.awt.Color;

@ConfigGroup("dialogue")
public interface DialogueConfig extends Config {
	@ConfigSection(
			name = "Ignore List",
			description = "Custom exclusion list",
			position = 0,
			closedByDefault = true
	)
	String ignoreListSection = "ignoreList";

	@ConfigSection(
			name = "Chatbox Dialogue",
			description = "All options for displaying dialogue in the chatbox",
			position = 1
	)
	String chatDialogueSection = "chatDialogue";

	@ConfigSection(
			name = "Overhead Dialogue",
			description = "All options for displaying overhead dialogue",
			position = 2
	)
	String overheadDialogueSection = "overheadDialogue";

	@ConfigItem(
			keyName = "displayChatboxNpcDialogue",
			name = "NPC Chatbox Dialogue",
			description = "Display NPC dialogue in the chatbox",
			section = chatDialogueSection,
			position = 0
	)
	default boolean displayChatboxNpcDialogue() {
		return true;
	}

	@ConfigItem(
			keyName = "displayChatboxPlayerDialogue",
			name = "Player Chatbox Dialogue",
			description = "Display player dialogue in the chatbox",
			section = chatDialogueSection,
			position = 1
	)
	default boolean displayChatboxPlayerDialogue() {
		return true;
	}

	@Alpha
	@ConfigItem(
			keyName = "nameColor",
			name = "Chatbox Name Color",
			description = "Select the display color for the name in the chatbox",
			section = chatDialogueSection,
			position = 2
	)
	default Color nameColor() {
		return Color.BLACK;
	}

	@Alpha
	@ConfigItem(
			keyName = "contentColor",
			name = "Chatbox Text Color",
			description = "Select the display color for the message text in the chatbox",
			section = chatDialogueSection,
			position = 3
	)
	default Color contentColor() {
		return Color.BLUE;
	}

	@ConfigItem(
			keyName = "displayOverheadNpcDialogue",
			name = "NPC Overhead Dialogue",
			description = "Display dialogue above the NPC",
			section = overheadDialogueSection,
			position = 0
	)
	default boolean displayOverheadNpcDialogue() {
		return true;
	}

	@ConfigItem(
			keyName = "displayOverheadPlayerDialogue",
			name = "Player Overhead Dialogue",
			description = "Display player dialogue overhead",
			section = overheadDialogueSection,
			position = 1
	)
	default boolean displayOverheadPlayerDialogue() {
		return true;
	}

	@ConfigItem(
			keyName = "ignoredNPCs",
			name = "Ignored NPCs",
			description = "List of ignored NPCs, separated by commas",
			position = 0,
			section = ignoreListSection
	)
	default String ignoredNPCs() {
		return "";
	}
}
