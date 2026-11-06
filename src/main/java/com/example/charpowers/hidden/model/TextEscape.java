package com.example.charpowers.hidden.model;

// Text put inside '...' in a Minecraft command (e.g. a floating note's JSON) must have \ and ' escaped,
// backslashes first, or quotes and backslashes in what the player typed break the command.
public final class TextEscape {
    public static String forSingleQuotes(String s) {
        return s.replace("\\", "\\\\").replace("'", "\\'");
    }

    private TextEscape() {}
}
