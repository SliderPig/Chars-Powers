package com.example.charpowers.vocal.model;

import java.util.Locale;
import java.util.Map;

// Turning typed text into solid letters, with no game code in it.
public final class WordRules {
    public static final int MESSAGES_NEEDED = 1000, GAP_TO_CROSS = 200, MAX_ENERGY = 100;
    public static final int HARD_HITS_LEVEL = 50, TWO_WORDS_LEVEL = 100;
    public static final int ROWS = 7, LETTER_WIDTH = 5, GAP = 1;

    // 7 rows of 5 pixels each, as 2 hex digits a row.
    private static final Map<Character, String> FONT = Map.ofEntries(
            Map.entry('A', "0E11111F111111"), Map.entry('B', "1E11111E11111E"), Map.entry('C', "0E11101010110E"),
            Map.entry('D', "1E11111111111E"), Map.entry('E', "1F10101E10101F"), Map.entry('F', "1F10101E101010"),
            Map.entry('G', "0E11101711110F"), Map.entry('H', "1111111F111111"), Map.entry('I', "0E04040404040E"),
            Map.entry('J', "0702020202120C"), Map.entry('K', "11121418141211"), Map.entry('L', "1010101010101F"),
            Map.entry('M', "111B1515111111"), Map.entry('N', "11191513111111"), Map.entry('O', "0E11111111110E"),
            Map.entry('P', "1E11111E101010"), Map.entry('Q', "0E11111115120D"), Map.entry('R', "1E11111E141211"),
            Map.entry('S', "0F10100E01011E"), Map.entry('T', "1F040404040404"), Map.entry('U', "1111111111110E"),
            Map.entry('V', "11111111110A04"), Map.entry('W', "11111115151B11"), Map.entry('X', "11110A040A1111"),
            Map.entry('Y', "11110A04040404"), Map.entry('Z', "1F01020408101F"), Map.entry('0', "0E11131519110E"),
            Map.entry('1', "040C040404040E"), Map.entry('2', "0E11010204081F"), Map.entry('3', "1F02040201110E"),
            Map.entry('4', "02060A121F0202"), Map.entry('5', "1F101E0101110E"), Map.entry('6', "0608101E11110E"),
            Map.entry('7', "1F010204080808"), Map.entry('8', "0E11110E11110E"), Map.entry('9', "0E11110F01020C"),
            Map.entry('!', "04040404040004"), Map.entry('?', "0E110102040004"), Map.entry('.', "00000000000C0C"),
            Map.entry(',', "000000000C0408"), Map.entry('\'', "04040800000000"), Map.entry('-', "0000001F000000"),
            Map.entry(' ', "00000000000000"));

    // Awakened words that do what they say (the code is in TrueWords).
    public static final java.util.List<String> TRUE_WORDS = java.util.List.of("BURN", "ICE", "BOOM", "FREEZE", "GROW", "BLOOM",
            "HEAL", "PUSH", "PULL", "LIGHT", "STORM", "SLEEP", "JUMP");

    public static boolean isTrueWord(String word) {
        return TRUE_WORDS.contains(word);
    }

    private WordRules() {}

    private static int steps(int level) {
        return Math.max(0, Math.min(99, level - 1));
    }

    // Longest word: 5 letters at level 1, 30 at level 100.
    public static int maxLetters(int level) {
        return 5 + steps(level) * 25 / 99;
    }

    // Words last 10 s at level 1, 5 minutes at level 100.
    public static int lifeTicks(int level) {
        return 200 + steps(level) * 5800 / 99;
    }

    // Bigger words use more energy, and each level uses a bit less: 4 a letter down to 2.
    public static int cost(int letters, int level) {
        return (int) Math.ceil(letters * (4.0 - steps(level) * 2.0 / 99.0));
    }

    public static float damage(int letters, int level) {
        float hit = 2f + letters * 0.5f;
        return level >= HARD_HITS_LEVEL ? hit * 1.5f : hit;
    }

    public static int wordsAtOnce(int level) {
        return level >= TWO_WORDS_LEVEL ? 2 : 1;
    }

    // Only what the font can draw, in capitals, cut to the limit. Spaces don't count as letters.
    public static String clean(String typed, int maxLetters) {
        StringBuilder out = new StringBuilder();
        int letters = 0;
        for (char c : typed.toUpperCase(Locale.ROOT).trim().toCharArray()) {
            if (!FONT.containsKey(c)) continue;
            if (c != ' ') {
                if (letters == maxLetters) break;
                letters++;
            }
            out.append(c);
        }
        return out.toString().trim();
    }

    public static int letters(String word) {
        return word.replace(" ", "").length();
    }

    public static int width(String word) {
        return word.isEmpty() ? 0 : word.length() * (LETTER_WIDTH + GAP) - GAP;
    }

    // Is there ink at this spot? Row 0 is the top of the letter.
    public static boolean ink(String word, int column, int row) {
        int letter = column / (LETTER_WIDTH + GAP), x = column % (LETTER_WIDTH + GAP);
        if (letter >= word.length() || x >= LETTER_WIDTH || row < 0 || row >= ROWS) return false;
        String glyph = FONT.get(word.charAt(letter));
        if (glyph == null) return false;
        int bits = Integer.parseInt(glyph.substring(row * 2, row * 2 + 2), 16);
        return (bits & (1 << (LETTER_WIDTH - 1 - x))) != 0;
    }
}
