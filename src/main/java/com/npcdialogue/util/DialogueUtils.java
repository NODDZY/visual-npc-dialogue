package com.npcdialogue.util;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class DialogueUtils {
    // Regex pattern to match parentheses and their contents at the end of the string
    private static final Pattern SUFFIX_PATTERN = Pattern.compile("\\s*\\([^)]*\\)\\s*$");

    /**
     * Remove suffixes from NPC names
     * @param name NPC name
     * @return name without suffixes
     * @see <a href="https://oldschool.runescape.wiki/w/Suffixes">Wiki: Suffixes</a>
     */
    public static String trimName(String name) {
        return SUFFIX_PATTERN.matcher(name).replaceAll("").trim();
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
        String trimmedName = trimName(name);
        for (String n : names) {
            String ignored = n.trim();
            if (ignored.equalsIgnoreCase(name) || ignored.equalsIgnoreCase(trimmedName)) {
                log.debug("NPC found in ignore list: {}", name);
                return true;
            }
        }
        return false;
    }
}
