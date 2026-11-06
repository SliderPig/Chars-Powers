package com.example.charpowers.cloning.jobs;

import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.List;

// The jobs you can order, by name (commands and the menu).
public final class Jobs {
    public static final List<String> NAMES = List.of("follow", "attack", "mine", "chop", "cook", "guard", "farm");

    private Jobs() {}

    // here = where the order was given
    @Nullable
    public static CloneJob make(String name, BlockPos here) {
        return switch (name) {
            case "follow" -> new FollowJob();
            case "attack" -> new AttackJob(here);
            case "mine" -> new MineJob();
            case "chop" -> new ChopJob(here);
            case "cook" -> new CookJob();
            case "guard" -> new GuardJob(here);
            case "farm" -> new FarmJob(here);
            default -> null;
        };
    }
}
