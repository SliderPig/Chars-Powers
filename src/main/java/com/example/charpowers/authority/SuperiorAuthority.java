package com.example.charpowers.authority;

import com.example.charpowers.authority.model.AuthorityRules;
import com.example.charpowers.authority.model.ThroneRule;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.own.OwnPower;
import com.example.charpowers.own.RaidWatch;
import com.example.charpowers.sound.PowerSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.power.ability.AbilityUtil;

// Make things happen that everyone must obey. You never have to obey, only others.
// The commands run in Orders. Here: the keys, and the two raids on a throne.
public final class SuperiorAuthority extends OwnPower {
    public static final SuperiorAuthority POWER = new SuperiorAuthority();

    private SuperiorAuthority() {
        super(Mastery.SUPERIOR_AUTHORITY, Items.GOLD_BLOCK);
    }

    @Override
    public boolean watches() { return true; }

    @Override
    public String unlockMessage() { return "You sat through a whole raid and they lost anyway. Everyone must obey you now."; }

    @Override
    public String awakenedMessage() { return "You won a raid without lifting a finger. You have one more word to say: Die."; }

    @Override
    public void onKey(ServerPlayer p, String ability) {
        if ("die".equals(ability)) {
            Orders.die(p);
            Fx.sound(p, PowerSounds.COMMAND_BOOM, 1.5f, 0.8f);
            tell(p, "Die.");
            return;
        }
        int mask = AuthorityRules.mask(ability);
        if (mask == 0) return;
        int level = mastery(p);
        int ticks = AuthorityRules.ticks(level);
        int range = AuthorityRules.range(level);
        LivingEntity victim = null;
        if (mask == AuthorityRules.ATTACK) {
            victim = Fx.lookTarget(p, 64);
            if (victim == null) {
                whisper(p, "Look at who they should attack.");
                return;
            }
        }
        Orders.start(p, AuthorityRules.word(ability), mask, range, ticks, victim);
        Fx.sound(p, PowerSounds.COMMAND_BOOM, 1f, 1f);
        whisper(p, AuthorityRules.word(ability) + ".");
        AbilityInstance key = AbilityUtil.getInstance(p, id, ability);
        if (key != null) key.startCooldown(p, ticks);
    }

    private static ThroneRule.Blocks blocksOf(Level level) {
        return new ThroneRule.Blocks() {
            @Override
            public boolean gold(int x, int y, int z) { return level.getBlockState(new BlockPos(x, y, z)).is(Blocks.GOLD_BLOCK); }

            @Override
            public boolean diamond(int x, int y, int z) { return level.getBlockState(new BlockPos(x, y, z)).is(Blocks.DIAMOND_BLOCK); }
        };
    }

    // Which way the back of the throne you're crouching on is, or -1.
    private static int sitting(ServerPlayer p, boolean superior) {
        if (!p.isCrouching() || !p.onGround()) return -1;
        BlockPos seat = p.blockPosition().below();
        if (!p.level().getBlockState(seat).is(Blocks.GOLD_BLOCK)) return -1;
        ThroneRule.Blocks blocks = blocksOf(p.level());
        return superior ? ThroneRule.superior(blocks, seat.getX(), seat.getY(), seat.getZ())
                : ThroneRule.gold(blocks, seat.getX(), seat.getY(), seat.getZ());
    }

    private static boolean backBehind(ServerPlayer p, int back) {
        Direction facing = p.getDirection();
        return facing.getStepX() == -ThroneRule.BACK[back][0] && facing.getStepZ() == -ThroneRule.BACK[back][1];
    }

    // How to get it: crouch on a gold throne for a WHOLE raid, and win.
    @Override
    public void watchSecond(ServerPlayer p) {
        throneRaid(p, false);
    }

    // The quest: the same on a superior throne, using only commands, never hitting anything yourself.
    @Override
    public void everySecond(ServerPlayer p) {
        if (questReady(p)) throneRaid(p, true);
    }

    private void throneRaid(ServerPlayer p, boolean quest) {
        int back = sitting(p, quest);
        if (!RaidWatch.inOne(p)) {
            if (back < 0 || !backBehind(p, back)) return;
            RaidWatch.Watch w = RaidWatch.step(p);
            if (w != null) whisper(p, "A raid is coming. Stay crouched on your throne for all of it.");
            return;
        }
        RaidWatch.Watch w = RaidWatch.step(p);
        if (w == null) return;
        if (w.over) {
            if (!w.won()) {
                tell(p, "They won. Try again next raid.");
            } else if (back < 0) {
                tell(p, "The raid was won, but you weren't on your throne.");
            } else if (quest && w.didHit) {
                tell(p, "You won, but you hit something yourself. That one doesn't count.");
            } else if (quest) {
                awaken(p);
            } else {
                unlock(p);
            }
            return;
        }
        if (back < 0) {
            RaidWatch.drop(p);
            tell(p, "You got up. Try again next raid.");
        }
    }
}
