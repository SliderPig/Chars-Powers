package com.example.charpowers.client.anim.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

// How the minigun sits in your hands: the "gun" part of player_poses.json. The origin is in player-model pixels from the point
// between your shoulders while looking level (x your left, y down, z back). The grips are in the gun model's own pixels, the
// numbers Blockbench shows.
public final class GunNumbers {
    public static final GunNumbers DEFAULTS = new GunNumbers(new double[] {-7, 16, -16}, 0.8, new double[] {4, 12, -16}, new double[] {0, 16, -8}, new double[] {-10.4, 5.8, -2.4}, -60, 42, new double[] {4.4, -3.5, -15}, new double[] {-13.4, 13, 15}, new double[] {2.6, 15, 13}, new double[] {0, 3, -6});

    public final double[] origin;
    public final double size;
    public final double[] gripRight, gripLeft;
    public final double[] pivot;      // what it tips round when you look up and down, from the same point as origin: near the back hand, so the stock stays put
    public final double pitchUp, pitchDown; // how far it tips with your look, in degrees: past that the look goes on without it
    // First person only, nobody else sees it: gun and hands are brought this far up into view and turn with the camera, and the arms come from
    // these spots below and behind the view instead of your real shoulders (which would leave the hands off the bottom of the screen).
    public final double[] firstPerson, firstPersonRight, firstPersonLeft;
    public final double[] firstPersonPoses; // the same for every other pose: real shoulders are too far back for your own arms to show

    public GunNumbers(double[] origin, double size, double[] gripRight, double[] gripLeft, double[] pivot, double pitchUp, double pitchDown, double[] firstPerson, double[] firstPersonRight, double[] firstPersonLeft, double[] firstPersonPoses) {
        this.origin = origin;
        this.size = size;
        this.gripRight = gripRight;
        this.gripLeft = gripLeft;
        this.pivot = pivot;
        this.pitchUp = pitchUp;
        this.pitchDown = pitchDown;
        this.firstPerson = firstPerson;
        this.firstPersonRight = firstPersonRight;
        this.firstPersonLeft = firstPersonLeft;
        this.firstPersonPoses = firstPersonPoses;
    }

    static GunNumbers read(JsonObject o) {
        return new GunNumbers(
                vec(o, "origin", DEFAULTS.origin),
                PoseFile.number(o, "size", DEFAULTS.size),
                vec(o, "gripRight", DEFAULTS.gripRight),
                vec(o, "gripLeft", DEFAULTS.gripLeft),
                vec(o, "pivot", DEFAULTS.pivot),
                PoseFile.number(o, "pitchUp", DEFAULTS.pitchUp),
                PoseFile.number(o, "pitchDown", DEFAULTS.pitchDown),
                vec(o, "firstPerson", DEFAULTS.firstPerson),
                vec(o, "firstPersonRight", DEFAULTS.firstPersonRight),
                vec(o, "firstPersonLeft", DEFAULTS.firstPersonLeft),
                vec(o, "firstPersonPoses", DEFAULTS.firstPersonPoses));
    }

    private static double[] vec(JsonObject o, String key, double[] otherwise) {
        if (!o.has(key)) return otherwise;
        JsonArray a = o.getAsJsonArray(key);
        return new double[] {a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()};
    }
}
