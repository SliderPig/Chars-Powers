package com.example.charpowers.hidden.fx;

import com.example.charpowers.CharPowers;
import net.minecraft.util.RandomSource;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

// Funny text you can rewrite: config/charpowers/<name>.txt, one line each.
// The first time, the file is made with the lines below. Edit it and restart to use your own.
public final class TextLists {
    private static final Map<String, List<String>> CACHE = new HashMap<>();

    public static final List<String> DAD_JOKES = List.of(
            "Why did the creeper cross the road? To get to the other SSSSIDE.",
            "I'd tell you a joke about dirt, but it's a bit too common.",
            "What do you call a zombie that won't stop talking? A dead giveaway.",
            "Why are skeletons so calm? Nothing gets under their skin.",
            "I used to be a banker, but I lost interest.",
            "What's a ghast's favourite country? The Nether-lands.",
            "Why don't endermen tell secrets? They always end up somewhere else.",
            "I'm reading a book about anti-gravity. I can't put it down.",
            "Why did the cow go to space? To see the moooon.",
            "What did the bee say to the flower? Hey bud, what's up?",
            "Why did the pig get kicked out of the game? It kept hogging the spotlight.",
            "I only know 25 letters of the alphabet. I don't know Y.",
            "What do you call a sleeping bull? A bulldozer.",
            "Why can't you trust stairs? They're always up to something.",
            "What's brown and sticky? A stick.",
            "Why did the sheep stop playing? It was feeling a little baaad.",
            "What do you call a fish with no eyes? A fsh.",
            "Why did the chicken join a band? It had the drumsticks.",
            "I told my cat a joke about lasers. It didn't get the point.",
            "What do you call an alligator in a vest? An investigator.",
            "Why don't eggs tell jokes? They'd crack each other up.",
            "Why did the scarecrow win an award? He was outstanding in his field.",
            "What do you call a pile of cats? A meowntain.",
            "Why did the golem go to therapy? It had a heavy heart of iron.",
            "How does a villager say hello? Hrmm.",
            "Why did the miner bring a ladder? To reach the high scores.",
            "What's a witch's favourite subject? Spelling.",
            "Why did the redstone break up? There was no spark.",
            "What do you call a bear with no teeth? A gummy bear.",
            "I would make a joke about bedrock, but you'd never break it.");

    public static final List<String> FORTUNES = List.of(
            "You will find diamonds... eventually.", "A creeper is thinking about you.", "Today is a good day to nap.",
            "Your next chest holds something nice.", "Beware of lava you can't see.", "A cat will choose you.",
            "Someone near you is hiding a secret power.", "Your bed is safer than you think.", "Rain brings mushrooms.",
            "The next mob you meet is friendly. Maybe.", "Plant a tree, gain a friend.", "Your pickaxe believes in you.",
            "Good luck comes in stacks of 64.", "An enderman respects your boundaries.", "Trade wisely, villager friend.",
            "Your house needs more windows.", "You will laugh before sunset.", "Gold is shiny, iron is useful.",
            "A fish will find you today.", "Your next jump will be your best one.", "Don't look up. Or do.",
            "Bring torches. Always.", "A pig is your lucky animal.", "You are one block away from greatness.",
            "Snacks solve most problems.", "The cave you fear holds the ore you seek.", "Someone will share food with you.",
            "Your shield will save you soon.", "Dream big, build bigger.", "Meow. (That's a good sign.)");

    // {name} is the player's name
    public static final List<String> NARRATOR = List.of(
            "{name} bravely eats a {food}.", "Here we see {name}, carefully choosing where to step.",
            "{name} pauses, perhaps remembering where they left their pickaxe.",
            "Deep underground, {name} hunts for shiny rocks.", "{name} swims gracefully. Mostly.",
            "Look at {name} run. Magnificent.", "{name} rests. A wise choice in the wild.",
            "{name} stands perfectly still. Is it a trap?", "{name} fights for survival, as nature intended.",
            "Notice how {name} looks around. Always alert.", "{name} jumps. For no clear reason.",
            "Night falls, and {name} wonders where the bed went.", "The rain falls on {name}. They do not mind.",
            "{name} sneaks. The cows suspect nothing.", "{name} has been here before. Probably.",
            "With great care, {name} places a block.", "{name} breaks a block. Destruction is natural.",
            "{name} admires the view.", "A rare sight: {name}, doing absolutely nothing.",
            "{name} is hungry. Soon, a snack.");

    public static List<String> get(String name, List<String> defaults) {
        return CACHE.computeIfAbsent(name, n -> load(n, defaults));
    }

    public static String random(String name, List<String> defaults, RandomSource r) {
        List<String> lines = get(name, defaults);
        return lines.isEmpty() ? "..." : lines.get(r.nextInt(lines.size()));
    }

    private static List<String> load(String name, List<String> defaults) {
        Path file = FMLPaths.CONFIGDIR.get().resolve("charpowers").resolve(name + ".txt");
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.write(file, defaults, StandardCharsets.UTF_8);
                return defaults;
            }
            List<String> out = new ArrayList<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) if (!line.isBlank()) out.add(line.trim());
            return out.isEmpty() ? defaults : out;
        } catch (IOException e) {
            CharPowers.LOG.warn("Couldn't read {}, using the built-in lines", file, e);
            return defaults;
        }
    }

    private TextLists() {}
}
