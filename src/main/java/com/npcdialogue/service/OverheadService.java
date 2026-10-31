package com.npcdialogue.service;

import com.npcdialogue.DialogueConfig;
import com.npcdialogue.model.Dialogue;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.client.util.ColorUtil;

import java.awt.Color;
import javax.inject.Inject;

@Slf4j
public class OverheadService {
    @Inject private Client client;
    @Inject private DialogueConfig config;

    public void setOverheadTextNpc(Actor npc, Dialogue dialogue) {
        npc.setOverheadText(colorOverheadText(truncate(dialogue.getText()), config.npcOverheadColor()));
        npc.setOverheadCycle(config.durationOverheadText());
        log.debug("Set overhead dialogue for {} to: {}", npc.getName(), dialogue.getText());
    }

    public void setOverheadTextPlayer(Dialogue dialogue) {
        Actor player = client.getLocalPlayer();
        player.setOverheadText(colorOverheadText(truncate(dialogue.getText()), config.playerOverheadColor()));
        player.setOverheadCycle(config.durationOverheadText());
        log.debug("Set overhead dialogue for player to: {}", dialogue.getText());
    }

    private static String colorOverheadText(String text, Color color) {
        if (color == null || color.equals(Color.YELLOW)) {
            return text;
        }

        return ColorUtil.wrapWithColorTag(text, color);
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
