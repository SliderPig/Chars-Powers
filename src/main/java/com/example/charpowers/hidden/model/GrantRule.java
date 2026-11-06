package com.example.charpowers.hidden.model;

// The Palladium power "Hidden Powers" is what puts the 5 keys on the ability bar. If anything takes that
// power away (DNA Normalizer, a GabeUp vial, /superpower remove) while the player still has hidden powers,
// that counts as clearing your powers: the whole collection goes. We never add it back by ourselves.
public final class GrantRule {
    public enum Action { NOTHING, WIPE }

    public static Action onCheck(boolean hasPalladiumPower, boolean collectionEmpty) {
        return !hasPalladiumPower && !collectionEmpty ? Action.WIPE : Action.NOTHING;
    }

    private GrantRule() {}
}
