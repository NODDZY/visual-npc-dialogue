package com.npcdialogue;

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup(DialogueConfig.CONFIG_GROUP)
public interface DialogueConfig extends Config {
    String CONFIG_GROUP = "visualnpcdialogue";

    @ConfigItem(
        keyName = "disableDialogueDuringCutscene",
        name = "Disable during cutscenes",
        description = "Disable chatbox and overhead dialogue during cutscenes",
        position = 1
    )
    default boolean disableDialogueDuringCutscene() {
        return true;
    }

    @ConfigSection(
            name = "Ignore List",
            description = "Custom exclusion list",
            position = 100,
            closedByDefault = true
    )
    String SECTION_IGNORE_LIST = "ignoreList";

    @ConfigItem(
        keyName = "ignoredNPCs",
        name = "Ignored NPCs",
        description = "NPCs to ignore, separated by commas. Use * as a wildcard.",
        position = 101,
        section = SECTION_IGNORE_LIST
    )
    default String ignoredNPCs() {
        return "";
    }

    @ConfigSection(
            name = "Chatbox Dialogue",
            description = "All options for displaying dialogue in the chatbox",
            position = 200
    )
    String SECTION_CHAT = "chatDialogue";

    @ConfigItem(
            keyName = "displayChatboxNpcDialogue",
            name = "NPC Chatbox Dialogue",
            description = "Display NPC dialogue in the chatbox",
            section = SECTION_CHAT,
            position = 201
    )
    default boolean displayChatboxNpcDialogue() {
        return true;
    }

    @ConfigItem(
            keyName = "displayChatboxPlayerDialogue",
            name = "Player Chatbox Dialogue",
            description = "Display player dialogue in the chatbox",
            section = SECTION_CHAT,
            position = 202
    )
    default boolean displayChatboxPlayerDialogue() {
        return true;
    }

    @Alpha
    @ConfigItem(
            keyName = "nameColor",
            name = "Chatbox Name Color",
            description = "Select the display color for the name in the chatbox",
            section = SECTION_CHAT,
            position = 203
    )
    default Color nameColor() {
        return Color.BLACK;
    }

    @Alpha
    @ConfigItem(
            keyName = "contentColor",
            name = "Chatbox Text Color",
            description = "Select the display color for the message text in the chatbox",
            section = SECTION_CHAT,
            position = 204
    )
    default Color contentColor() {
        return Color.BLUE;
    }

    @ConfigSection(
            name = "Overhead Dialogue",
            description = "All options for displaying overhead dialogue",
            position = 300
    )
    String SECTION_OVERHEAD = "overheadDialogue";

    @ConfigItem(
            keyName = "displayOverheadNpcDialogue",
            name = "NPC Overhead Dialogue",
            description = "Display dialogue above the NPC",
            section = SECTION_OVERHEAD,
            position = 301
    )
    default boolean displayOverheadNpcDialogue() {
        return true;
    }

    @ConfigItem(
            keyName = "displayOverheadPlayerDialogue",
            name = "Player Overhead Dialogue",
            description = "Display player dialogue overhead",
            section = SECTION_OVERHEAD,
            position = 302
    )
    default boolean displayOverheadPlayerDialogue() {
        return true;
    }

    @Alpha
    @ConfigItem(
            keyName = "playerOverheadColor",
            name = "Overhead Color",
            description = "Select the color of overhead player dialogue<br>Only affects overhead dialogue created by this plugin",
            section = SECTION_OVERHEAD,
            position = 303
    )
    default Color playerOverheadColor() {
        return Color.YELLOW;
    }

    @Alpha
    @ConfigItem(
            keyName = "npcOverheadColor",
            name = "Overhead NPC Color",
            description = "Select the color of overhead NPC dialogue<br>Only affects overhead dialogue created by this plugin",
            section = SECTION_OVERHEAD,
            position = 304
    )
    default Color npcOverheadColor() {
        return Color.YELLOW;
    }

    @ConfigItem(
        keyName = "durationOverheadText",
        name = "Overhead Duration",
        description = "How long overhead text lasts. 30 cycles = 1 game tick (~0.6s).<br>Default: 100 (~3.3 ticks)",
        section = SECTION_OVERHEAD,
        position = 305
    )
    default int durationOverheadText() {
        return 100;
    }

    @ConfigItem(
        keyName = "truncateOverheadText",
        name = "Truncate Overhead",
        description = "Truncate overhead text after X characters<br>Set to 0 to disable",
        section = SECTION_OVERHEAD,
        position = 306
    )
    default int truncateOverheadText() {
        return 0;
    }
}
