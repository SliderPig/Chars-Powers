package com.example.charpowers.icons;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

// Every hidden power has its own picture, and every picture a power file names is there: 32x32, not blank, no two the same.
// (Fixed list of powers = the descriptions in the language file; a new power with no picture fails here.)
class IconFilesTest {
    private static final Path RES = Path.of("src/main/resources");
    private static final Path ICONS = RES.resolve("assets/charpowers/textures/icons");

    @Test
    void everyHiddenPowerHasItsOwnPicture() throws IOException {
        Pattern desc = Pattern.compile("\"power\\.charpowers\\.([a-z0-9_]+)\\.desc\"");
        Matcher m = desc.matcher(Files.readString(RES.resolve("assets/charpowers/lang/en_us.json")));
        List<String> problems = new ArrayList<>();
        Map<Long, String> seen = new HashMap<>();
        int n = 0;
        while (m.find()) {
            n++;
            look(ICONS.resolve("hidden").resolve(m.group(1) + ".png"), m.group(1), problems, seen);
        }
        assertTrue(n >= 381, "only found " + n + " hidden powers");
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void everyPictureAPowerFileNamesIsThere() throws IOException {
        Pattern texture = Pattern.compile("\"texture\": \"charpowers:textures/icons/([a-z0-9_/]+)\\.png\"");
        List<String> problems = new ArrayList<>();
        Map<Long, String> seen = new HashMap<>();
        Set<String> done = new HashSet<>();
        try (Stream<Path> files = Files.list(RES.resolve("data/charpowers/palladium/powers"))) {
            for (Path file : files.toList()) {
                Matcher m = texture.matcher(Files.readString(file));
                while (m.find()) {
                    if (done.add(m.group(1))) look(ICONS.resolve(m.group(1) + ".png"), file.getFileName() + " names " + m.group(1), problems, seen);
                }
            }
        }
        assertTrue(done.size() > 100, "only " + done.size() + " pictures named");
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    private static void look(Path file, String name, List<String> problems, Map<Long, String> seen) throws IOException {
        if (!Files.exists(file)) {
            problems.add(name + ": no picture");
            return;
        }
        BufferedImage im = ImageIO.read(file.toFile());
        if (im.getWidth() != 32 || im.getHeight() != 32) {
            problems.add(name + ": is " + im.getWidth() + "x" + im.getHeight());
            return;
        }
        long hash = 7;
        int solid = 0;
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                int argb = im.getRGB(x, y);
                if (argb >>> 24 != 0) solid++;
                hash = hash * 1000003 + argb;
            }
        }
        if (solid < 60) problems.add(name + ": nearly blank");
        String other = seen.putIfAbsent(hash, name);
        if (other != null) problems.add(name + ": the same picture as " + other);
    }
}
