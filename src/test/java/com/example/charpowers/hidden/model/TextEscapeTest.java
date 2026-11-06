package com.example.charpowers.hidden.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextEscapeTest {
    @Test
    void quotesAndBackslashesSurviveSingleQuotes() {
        // JSON for the note:  say "hi" it's me \o/
        String json = "{\"text\":\"say \\\"hi\\\" it's me \\\\o/\"}";
        String out = TextEscape.forSingleQuotes(json);
        assertEquals("{\"text\":\"say \\\\\"hi\\\\\" it\\'s me \\\\\\\\o/\"}", out);
    }

    @Test
    void plainTextIsUnchanged() {
        assertEquals("{\"text\":\"hello\"}", TextEscape.forSingleQuotes("{\"text\":\"hello\"}"));
    }
}
