package com.npcdialogue.model;

import lombok.Data;

@Data
public class Dialogue {
    public enum Type { NPC, PLAYER }

    private final String name;
    private final String text;
    private final Type type;
}
