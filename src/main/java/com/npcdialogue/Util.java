package com.npcdialogue;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class Util {

    /**
     * Remove suffixes from NPC names.
     * See {@code https://oldschool.runescape.wiki/w/Suffixes } for affected NPCs.
     * @param name NPC name
     * @return NPC name without suffixes
     */
    public String trimName(String name) {
        // Regex pattern to match parentheses and their contents at the end of the string
        Pattern pattern = Pattern.compile("\\s*\\([^)]*\\)\\s*$");
        Matcher matcher = pattern.matcher(name);
        return matcher.replaceAll("");
    }

    /**
     * Check if actor is listed in the NPC ignore list
     *
     * @param name The NPC name to check
     * @return Whether actor is in the ignore list
     */
    public boolean isIgnoredActor(String ignoreList, String name) {
        if (name == null || ignoreList == null) { return false; }
        // Loop through Ignore List and look for NPC name
        String[] names = ignoreList.split(",");
        for (String n : names) {
            if (n.trim().equals(name) || trimName(name).equals(n)) {
                log.debug("NPC found in ignore list: " + name);
                return true;
            }
        }
        return false;
    }
}
