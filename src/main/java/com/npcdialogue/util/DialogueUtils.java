package com.npcdialogue.util;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class DialogueUtils {
    /**
     * Remove suffixes from NPC names
     * @param name NPC name
     * @return name without suffixes
     * @see <a href="https://oldschool.runescape.wiki/w/Suffixes">Wiki: Suffixes</a>
     */
    public static String trimName(String name) {
        // Regex pattern to match parentheses and their contents at the end of the string
        Pattern pattern = Pattern.compile("\\s*\\([^)]*\\)\\s*$");
        Matcher matcher = pattern.matcher(name);
        return matcher.replaceAll("");
    }

    /**
     * Check if actor is listed in the NPC ignore list
     * @param name The NPC name to check
     * @return Whether actor is in the ignore list
     */
    public static boolean isIgnoredActor(String ignoreList, String name) {
        if (name == null || ignoreList == null) {
            return false;
        }
        // Loop through Ignore List and look for NPC name
        String[] names = ignoreList.split(",");
        for (String n : names) {
            if (n.trim().equals(name) || trimName(name).equals(n)) {
                log.debug("NPC found in ignore list: {}", name);
                return true;
            }
        }
        return false;
    }
}
