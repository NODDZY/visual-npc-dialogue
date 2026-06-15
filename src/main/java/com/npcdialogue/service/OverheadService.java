package com.npcdialogue.service;

import com.npcdialogue.DialogueConfig;
import com.npcdialogue.model.Dialogue;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;

import javax.inject.Inject;

@Slf4j
public class OverheadService {
    @Inject private Client client;
    @Inject private DialogueConfig config;

    public void setOverheadTextNpc(Actor npc, Dialogue dialogue) {
        npc.setOverheadText(truncate(dialogue.getText()));
        npc.setOverheadCycle(config.durationOverheadText());
        log.debug("Set overhead dialogue for {} to: {}", npc.getName(), dialogue.getText());
    }

    public void setOverheadTextPlayer(Dialogue dialogue) {
        Actor player = client.getLocalPlayer();
        player.setOverheadText(truncate(dialogue.getText()));
        player.setOverheadCycle(config.durationOverheadText());
        log.debug("Set overhead dialogue for player to: {}", dialogue.getText());
    }

    private String truncate(String text) {
        int maxLength = config.truncateOverheadText();

        if (maxLength <= 0 || text == null || text.length() <= maxLength) {
            return text;
        } else if (maxLength <= 3) {
            return text.substring(0, maxLength);
        }

        return text.substring(0, maxLength - 3) + "...";
    }
}
