package com.npcdialogue.service;

import com.npcdialogue.model.Dialogue;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;

import javax.inject.Inject;

@Slf4j
public class OverheadService {
    @Inject private Client client;

    public void setOverheadTextNpc(Actor npc, Dialogue dialogue) {
        npc.setOverheadText(dialogue.getText());
        log.debug("Set overhead dialogue for {} to: {}", npc.getName(), dialogue.getText());
    }

    public void setOverheadTextPlayer(Dialogue dialogue) {
        client.getLocalPlayer().setOverheadText(dialogue.getText());
        log.debug("Set overhead dialogue for player to: {}", dialogue.getText());
    }

    public void clearOverheadText(Actor actor) {
        actor.setOverheadText(null);
    }
}
