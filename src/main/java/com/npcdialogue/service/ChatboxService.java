package com.npcdialogue.service;

import com.npcdialogue.DialogueConfig;
import com.npcdialogue.model.Dialogue;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;

import javax.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ChatboxService {
    @Inject
    private DialogueConfig config;

    @Inject
    private ChatMessageManager chatMessageManager;

    /**
     * Adds NPC/Player dialogue to chatbox
     */
    public void addDialogMessage(Dialogue dialogue) {
        final ChatMessageBuilder chatMessage = new ChatMessageBuilder()
                .append(config.nameColor(), trimName(dialogue.getName()))
                .append(config.nameColor(), ": ")
                .append(config.contentColor(), dialogue.getContent());

        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.CONSOLE)
                .runeLiteFormattedMessage(chatMessage.build())
                .build());

        log.debug("Chatbox dialogue built and queued: " + dialogue);
    }

    private String trimName(String name) {
        // Regex pattern to match parentheses and their contents at the end of the string
        Pattern pattern = Pattern.compile("\\s*\\([^)]*\\)\\s*$");
        Matcher matcher = pattern.matcher(name);
        return matcher.replaceAll("");
    }

}
