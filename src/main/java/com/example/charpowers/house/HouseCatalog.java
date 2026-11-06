package com.example.charpowers.house;

import com.example.charpowers.CharPowers;
import com.example.charpowers.house.model.HouseList;
import com.example.charpowers.house.model.SchematicReader;
import com.example.charpowers.house.model.SidecarText;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

// Every house the power knows: the ten built in, and any building files in config/charpowers/houses: .nbt, .schem, .litematic, .schematic.
// Files are read away from the main thread, so a big one never stops the server, and an optional <name>.txt beside one says  level=25  name=Big Barn.
public final class HouseCatalog {
    private static final Path FOLDER = FMLPaths.CONFIGDIR.get().resolve("charpowers/houses");
    private static final int MAX_FILES = 64;
    private static final Map<String, HousePlan> BUILT_IN = new HashMap<>();
    private static volatile Map<String, HousePlan> custom = Map.of();
    private static final Map<String, Long> STAMPS = new HashMap<>();
    private static final AtomicBoolean READING = new AtomicBoolean();
    private static volatile boolean scanned;
    private static volatile List<String> problems = List.of();
    private static long lastLook;

    private HouseCatalog() {}

    public static List<HousePlan> all(MinecraftServer server) {
        loadBuiltIn(server);
        // new files turn up on their own, without anybody typing anything
        long now = System.currentTimeMillis();
        if (now - lastLook > 5000) {
            lastLook = now;
            scan(server, false, null);
        }
        List<HousePlan> out = new ArrayList<>();
        for (HouseList.Entry e : HouseList.BUILT_IN) {
            HousePlan plan = BUILT_IN.get(e.id());
            if (plan != null) out.add(plan);
        }
        custom.values().stream().sorted(Comparator.comparingInt((HousePlan p) -> p.level).thenComparing(p -> p.name)).forEach(out::add);
        return out;
    }

    @Nullable
    public static HousePlan get(MinecraftServer server, String id) {
        loadBuiltIn(server);
        HousePlan plan = BUILT_IN.get(id);
        return plan != null ? plan : custom.get(id);
    }

    // A saved job for one of the owner's files has to wait for them to be read after a restart.
    public static boolean ready() {
        return scanned;
    }

    public static List<String> problems() {
        return problems;
    }

    public static int customCount() {
        return custom.size();
    }

    // A world closed: a scan still under way will never get to report back, and the next world starts fresh.
    public static void reset() {
        READING.set(false);
        scanned = false;
        custom = Map.of();
        STAMPS.clear();
        problems = List.of();
        lastLook = 0;
    }

    private static void loadBuiltIn(MinecraftServer server) {
        if (!BUILT_IN.isEmpty()) return;
        Set<String> skipped = new TreeSet<>();
        for (HouseList.Entry e : HouseList.BUILT_IN) {
            ResourceLocation at = new ResourceLocation(CharPowers.MODID, "structures/houses/" + e.id() + ".nbt");
            try (InputStream in = server.getResourceManager().getResourceOrThrow(at).open()) {
                CompoundTag root = SchematicReader.readTag(in);
                String name = root.getCompound("charpowers").getString("name");
                SchematicReader.Result r = SchematicReader.vanilla(root);
                HousePlan plan = HousePlan.of(e.id(), name.isEmpty() ? e.id() : name, e.level(), e.awakened(), false, r, text -> HouseBlocks.resolve(text, 0, skipped));
                if (plan != null) BUILT_IN.put(e.id(), plan);
                else CharPowers.LOG.error("House file {} has nothing usable in it", at);
            } catch (IOException | RuntimeException ex) {
                CharPowers.LOG.error("Couldn't read the house {}", at, ex);
            }
        }
        if (!skipped.isEmpty()) CharPowers.LOG.error("Built-in houses name blocks the game doesn't have: {}", skipped);
    }

    // Reads whatever changed (everything if forced) on a background thread. `done` hears the result back on the server thread.
    public static void scan(MinecraftServer server, boolean force, @Nullable Consumer<String> done) {
        if (!READING.compareAndSet(false, true)) {
            if (done != null) done.accept("Still reading the last lot of files: try again in a moment.");
            return;
        }
        Util.backgroundExecutor().execute(() -> {
            Map<String, HousePlan> next = new HashMap<>();
            Map<String, Long> stamps = new HashMap<>();
            List<String> troubles = new ArrayList<>();
            try {
                readAll(force, next, stamps, troubles);
            } catch (RuntimeException | IOException | OutOfMemoryError e) {
                CharPowers.LOG.error("Couldn't read {}", FOLDER, e);
                troubles.add("the folder itself: " + e.getMessage());
            }
            server.execute(() -> {
                custom = next;
                STAMPS.clear();
                STAMPS.putAll(stamps);
                if (force || !troubles.isEmpty() || problems.isEmpty()) problems = troubles;
                scanned = true;
                READING.set(false);
                if (done != null) {
                    done.accept(next.size() + " own house file(s) ready" + (troubles.isEmpty() ? "." : ", " + troubles.size() + " with problems: " + String.join("; ", troubles)));
                }
            });
        });
    }

    private static void readAll(boolean force, Map<String, HousePlan> next, Map<String, Long> stamps, List<String> troubles) throws IOException {
        if (!Files.isDirectory(FOLDER)) return;
        List<Path> files;
        try (Stream<Path> listing = Files.list(FOLDER)) {
            files = listing.filter(p -> SchematicReader.canRead(p.getFileName().toString()) && Files.isRegularFile(p)).sorted().toList();
        }
        if (files.size() > MAX_FILES) {
            troubles.add("only the first " + MAX_FILES + " of " + files.size() + " files are used");
            files = files.subList(0, MAX_FILES);
        }
        Map<String, HousePlan> before = custom;
        for (Path file : files) {
            String id = idOf(file);
            String label = file.getFileName().toString();
            try {
                Path side = sidecar(file);
                long stamp = Files.getLastModifiedTime(file).toMillis() + (Files.isRegularFile(side) ? Files.getLastModifiedTime(side).toMillis() : 0);
                stamps.put(id, stamp);
                if (!force && Long.valueOf(stamp).equals(STAMPS.get(id))) {
                    // unchanged: keep it, or if it was refused last time, don't try again until it changes
                    if (before.containsKey(id)) next.put(id, before.get(id));
                    continue;
                }
                HousePlan plan = readOne(file, id, side, troubles);
                if (plan != null) next.put(id, plan);
            } catch (SchematicReader.Bad e) {
                CharPowers.LOG.warn("Not using the house file {}: {}", label, e.getMessage());
                troubles.add(label + ": " + e.getMessage());
            } catch (IOException | RuntimeException | OutOfMemoryError e) {
                CharPowers.LOG.warn("Couldn't read the house file {}", label, e);
                troubles.add(label + ": couldn't read it (" + e.getClass().getSimpleName() + ")");
            }
        }
    }

    private static HousePlan readOne(Path file, String id, Path side, List<String> troubles) throws IOException {
        String label = file.getFileName().toString();
        SchematicReader.Result r = SchematicReader.read(file, HouseBlocks::legacy);
        SidecarText text = new SidecarText(1, null);
        if (Files.isRegularFile(side) && Files.size(side) < 4096) {
            text = SidecarText.parse(Files.readAllLines(side, StandardCharsets.UTF_8));
        }
        Set<String> skipped = new TreeSet<>();
        HousePlan plan = HousePlan.of(id, text.name() != null ? text.name() : nameOf(file), text.level(), false, true, r, s -> HouseBlocks.resolve(s, r.dataVersion(), skipped));
        if (!skipped.isEmpty()) CharPowers.LOG.warn("House file {}: left out blocks that are unknown here or not allowed: {}", label, skipped);
        if (r.unmapped() > 0) CharPowers.LOG.warn("House file {}: {} old numbered blocks have no modern name and were left out", label, r.unmapped());
        if (plan == null) {
            troubles.add(label + ": nothing in it could be placed");
            return null;
        }
        CharPowers.LOG.info("House file {}: {} blocks, {} x {} wide, {} high", label, plan.cells.size(), plan.w, plan.d, plan.h);
        return plan;
    }

    private static Path sidecar(Path file) {
        String name = file.getFileName().toString();
        return file.resolveSibling(name.substring(0, name.lastIndexOf('.')) + ".txt");
    }

    private static String idOf(Path p) {
        String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        return "custom_" + name.substring(0, dot).replaceAll("[^a-z0-9_]", "_") + "_" + name.substring(dot + 1);
    }

    private static String nameOf(Path p) {
        String name = p.getFileName().toString();
        name = name.substring(0, name.lastIndexOf('.')).replace('_', ' ').replace('-', ' ').trim();
        if (name.length() > 40) name = name.substring(0, 40);
        return name.isEmpty() ? "House" : Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
