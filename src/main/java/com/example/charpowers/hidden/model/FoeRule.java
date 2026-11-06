package com.example.charpowers.hidden.model;

// Who a hidden power may hit. The same rule for a player's powers and a mob's (user: "everything unless they're on
// a team", "no discriminatory"). `mine` = summoned or tamed by the holder; `allied` = on the holder's /team;
// `alliedOwnersPet` = a tamed pet whose owner is on the holder's team.
public final class FoeRule {
    public record Facts(boolean living, boolean self, boolean alive, boolean armourStand, boolean creativeOrSpectator,
                        boolean mine, boolean allied, boolean alliedOwnersPet) {}

    public static boolean hittable(Facts f) {
        return f.living() && !f.self() && f.alive() && !f.armourStand() && !f.creativeOrSpectator()
                && !f.mine() && !f.allied() && !f.alliedOwnersPet();
    }

    private FoeRule() {}
}
