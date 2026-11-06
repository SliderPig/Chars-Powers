package com.example.charpowers.icons;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

// Every made sound listed in sounds.json has its file, its subtitle and its line in PowerSounds.
class SoundFilesTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/charpowers");

    @Test
    void everyMadeSoundIsComplete() throws IOException {
        String json = Files.readString(ASSETS.resolve("sounds.json"));
        String lang = Files.readString(ASSETS.resolve("lang/en_us.json"));
        String code = Files.readString(Path.of("src/main/java/com/example/charpowers/sound/PowerSounds.java"));
        Matcher m = Pattern.compile("\"name\": \"charpowers:powers/([a-z_]+)\"").matcher(json);
        List<String> problems = new ArrayList<>();
        int n = 0;
        while (m.find()) {
            n++;
            String name = m.group(1);
            Path file = ASSETS.resolve("sounds/powers/" + name + ".ogg");
            if (!Files.exists(file) || Files.size(file) < 1000) problems.add(name + ": no sound file");
            if (!lang.contains("\"sounds.charpowers." + name + "\"")) problems.add(name + ": no subtitle");
            if (!code.contains("make(\"" + name + "\")")) problems.add(name + ": not in PowerSounds");
        }
        assertTrue(n >= 20, "only found " + n + " made sounds");
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }
}
