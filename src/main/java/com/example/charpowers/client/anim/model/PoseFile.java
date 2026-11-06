package com.example.charpowers.client.anim.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Reads player_poses.json. Every keyframe says what its parts are doing at that tick; a part left out of a keyframe just carries on
// to the next one that names it. Parts: spine (lean forward, twist right, lean left), head (added to where you look),
// ra / la (right and left arm), rl / ll (legs), root (the whole body tips: forward, turn right, lean left), lift (the whole body
// moves: left, down, back, in model pixels). All angles in degrees.
public final class PoseFile {
    public final Map<String, PoseDef> poses;
    public final GunNumbers gun;
    public final boolean hasGun;

    private PoseFile(Map<String, PoseDef> poses, GunNumbers gun, boolean hasGun) {
        this.poses = poses;
        this.gun = gun;
        this.hasGun = hasGun;
    }

    public PoseFile withGun(GunNumbers numbers) {
        return new PoseFile(poses, numbers, true);
    }

    public static PoseFile read(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        Map<String, PoseDef> poses = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("poses").entrySet()) {
            String name = e.getKey().toLowerCase(Locale.ROOT);
            poses.put(name, pose(name, e.getValue().getAsJsonObject()));
        }
        boolean hasGun = root.has("gun");
        return new PoseFile(poses, hasGun ? GunNumbers.read(root.getAsJsonObject("gun")) : GunNumbers.DEFAULTS, hasGun);
    }

    static PoseDef pose(String name, JsonObject o) {
        Map<PoseDef.Part, List<double[]>> rows = new EnumMap<>(PoseDef.Part.class);
        Map<PoseDef.Part, List<Ease>> eases = new EnumMap<>(PoseDef.Part.class);
        double last = 0;
        for (JsonElement f : o.getAsJsonArray("frames")) {
            JsonObject frame = f.getAsJsonObject();
            double t = frame.get("t").getAsDouble();
            last = Math.max(last, t);
            Ease ease = Ease.named(frame.has("ease") ? frame.get("ease").getAsString() : null);
            for (PoseDef.Part part : PoseDef.Part.values()) {
                if (!frame.has(part.key)) continue;
                JsonArray v = frame.getAsJsonArray(part.key);
                if (v.size() != 3) throw new IllegalArgumentException(name + ": " + part.key + " needs three numbers");
                rows.computeIfAbsent(part, x -> new ArrayList<>()).add(new double[] {t, v.get(0).getAsDouble(), v.get(1).getAsDouble(), v.get(2).getAsDouble()});
                eases.computeIfAbsent(part, x -> new ArrayList<>()).add(ease);
            }
        }
        Map<PoseDef.Part, Track> tracks = new EnumMap<>(PoseDef.Part.class);
        for (PoseDef.Part part : rows.keySet()) {
            List<double[]> list = rows.get(part);
            double[] ticks = new double[list.size()];
            double[][] values = new double[list.size()][];
            // angles in the file are degrees, lift is already pixels
            double k = part == PoseDef.Part.LIFT ? 1 : Math.PI / 180;
            for (int i = 0; i < ticks.length; i++) {
                double[] row = list.get(i);
                ticks[i] = row[0];
                values[i] = new double[] {row[1] * k, row[2] * k, row[3] * k};
            }
            tracks.put(part, new Track(ticks, values, eases.get(part).toArray(new Ease[0])));
        }
        double loopFrom = 0, loopTo = 0;
        if (o.has("loop")) {
            JsonArray l = o.getAsJsonArray("loop");
            loopFrom = l.get(0).getAsDouble();
            loopTo = l.get(1).getAsDouble();
        }
        return new PoseDef(name,
                number(o, "ticks", last),
                number(o, "in", 3),
                number(o, "out", 6),
                loopFrom, loopTo,
                number(o, "follow", 1),
                flag(o, "firstPerson", true),
                flag(o, "additive", false),
                flag(o, "stopOnMove", false),
                flag(o, "stopOnHit", false),
                flag(o, "items", true),
                o.has("prop") ? o.get("prop").getAsString() : null,
                tracks);
    }

    static double number(JsonObject o, String key, double otherwise) {
        return o.has(key) ? o.get(key).getAsDouble() : otherwise;
    }

    static boolean flag(JsonObject o, String key, boolean otherwise) {
        return o.has(key) ? o.get(key).getAsBoolean() : otherwise;
    }
}
