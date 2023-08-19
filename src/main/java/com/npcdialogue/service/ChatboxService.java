package com.npcdialogue.service;

import com.npcdialogue.DialogueConfig;
import com.npcdialogue.model.Dialogue;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;

import javax.inject.Inject;

@Slf4j
public class ChatboxService {

    @Inject
    private DialogueConfig config;

    @Inject
    private ChatMessageManager chatMessageManager;

    /**
     * Adds NPC/Player dialogue to chatbox
     */
    public void addDialogMessage(Dialogue dialogue)
    {
        final ChatMessageBuilder chatMessage = new ChatMessageBuilder()
                .append(config.nameColor(), dialogue.getName())
                .append(config.nameColor(), ": ")
                .append(config.contentColor(), dialogue.getContent());

        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.CONSOLE)
                .runeLiteFormattedMessage(chatMessage.build())
                .build());

        log.debug("Chatbox dialogue built: " + dialogue);
    }

}
