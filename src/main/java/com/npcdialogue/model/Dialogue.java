package com.npcdialogue.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Dialogue {
    private final String name;
    private final String content;
}
