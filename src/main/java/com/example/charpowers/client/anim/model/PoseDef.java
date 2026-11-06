package com.example.charpowers.client.anim.model;

import java.util.EnumMap;
import java.util.Map;

// One named pose from player_poses.json: its keyframes and how it may be played.
public final class PoseDef {
    public enum Part {
        SPINE("spine"), HEAD("head"), RIGHT_ARM("ra"), LEFT_ARM("la"), RIGHT_LEG("rl"), LEFT_LEG("ll"), ROOT("root"), LIFT("lift");

        public final String key;

        Part(String key) {
            this.key = key;
        }
    }

    public final String name;
    public final double length;     // how long it runs, in ticks, before it fades out
    public final double blendIn, blendOut;
    public final double loopFrom, loopTo; // past loopTo it goes round from loopFrom until the length is up. loopTo <= loopFrom = no loop
    public final double follow;     // 1 = the arms and chest turn with where you look, 0 = they don't
    public final boolean firstPerson; // shows real arms in first person
    public final boolean additive;  // adds to what the body is already doing instead of replacing it
    public final boolean stopOnMove, stopOnHit;
    public final boolean drawItems; // what you hold stays in your hand in first person
    public final String prop;       // something drawn in the hands: "book", or null
    public final Map<Part, Track> tracks;

    public PoseDef(String name, double length, double blendIn, double blendOut, double loopFrom, double loopTo, double follow,
                   boolean firstPerson, boolean additive, boolean stopOnMove, boolean stopOnHit, boolean drawItems, Map<Part, Track> tracks) {
        this(name, length, blendIn, blendOut, loopFrom, loopTo, follow, firstPerson, additive, stopOnMove, stopOnHit, drawItems, null, tracks);
    }

    public PoseDef(String name, double length, double blendIn, double blendOut, double loopFrom, double loopTo, double follow,
                   boolean firstPerson, boolean additive, boolean stopOnMove, boolean stopOnHit, boolean drawItems, String prop, Map<Part, Track> tracks) {
        this.name = name;
        this.length = length;
        this.blendIn = blendIn;
        this.blendOut = blendOut;
        this.loopFrom = loopFrom;
        this.loopTo = loopTo;
        this.follow = follow;
        this.firstPerson = firstPerson;
        this.additive = additive;
        this.stopOnMove = stopOnMove;
        this.stopOnHit = stopOnHit;
        this.drawItems = drawItems;
        this.prop = prop;
        this.tracks = tracks.isEmpty() ? new EnumMap<>(Part.class) : new EnumMap<>(tracks);
    }

    // Where in the keyframes this much running time lands, going round the loop if it has one.
    public double keyTime(double running) {
        if (loopTo <= loopFrom || running <= loopTo) return running;
        double span = loopTo - loopFrom;
        return loopFrom + (running - loopFrom) % span;
    }

    // How much the pose counts at this running time: 0 to 1, soft at both ends. stopAt >= 0 means it was told to fade from then.
    public double weight(double running, double stopAt) {
        if (running < 0) return 0;
        double ending = ending(stopAt);
        // once it's told to stop it stops coming in, so a stop while still fading in can only go down from there
        double in = blendIn <= 0 ? 1 : Ease.SINE.at(Math.min(running, ending) / blendIn);
        double out = 1;
        if (running >= ending) out = blendOut <= 0 ? 0 : 1 - Ease.SINE.at((running - ending) / blendOut);
        return in * out;
    }

    public boolean finished(double running, double stopAt) {
        return running >= ending(stopAt) + blendOut;
    }

    private double ending(double stopAt) {
        return stopAt >= 0 && stopAt < length ? stopAt : length;
    }
}
