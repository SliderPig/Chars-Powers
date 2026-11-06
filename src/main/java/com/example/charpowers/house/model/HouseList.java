package com.example.charpowers.house.model;

import java.util.List;

// The built-in houses, in the order the picker shows them. Written by tools/make_houses.py as structure files.
public final class HouseList {
    public record Entry(String id, int level, boolean awakened) {}

    public static final List<Entry> BUILT_IN = List.of(
            new Entry("cosy_cottage", 1, false),
            new Entry("oak_timber_house", 25, false),
            new Entry("mountain_lodge", 50, false),
            new Entry("farmhouse", 75, false),
            new Entry("tudor_manor", 100, false),
            new Entry("pagoda_house", 1, true),
            new Entry("wizard_tower", 1, true),
            new Entry("canal_house", 1, true),
            new Entry("cherry_villa", 1, true),
            new Entry("towered_manor", 1, true));

    private HouseList() {}
}
