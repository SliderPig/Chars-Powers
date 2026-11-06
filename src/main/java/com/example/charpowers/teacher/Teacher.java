package com.example.charpowers.teacher;

import com.example.charpowers.own.Setting;
import com.example.charpowers.CharPowers;
import com.example.charpowers.cloning.CloneManager;
import com.example.charpowers.hidden.HiddenGrant;
import com.example.charpowers.hidden.PlayerHiddenData;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.own.OwnPower;
import com.example.charpowers.teacher.model.ClassRule;
import com.example.charpowers.teacher.model.TeacherRules;
import com.example.charpowers.sound.PowerSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.SuperpowerUtil;

import java.util.ArrayList;

// The joke power. You can teach anyone anything, but you can't have anything to teach. Everything else the player
// has is put away while they're a Teacher, and comes back when they quit (or something else takes Teacher away).
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class Teacher extends OwnPower {
    public static final Teacher POWER = new Teacher();

    private static final String COUNTED = "charpowers.teacher.counted";
    private static final String PARKED = "parked", PARKED_HIDDEN = "parkedHidden", MASTERS = "masters", CLICK = "click",
            SECONDS = "seconds", AWAY = "away", LECTERN = "lectern", WARNED = "warned";

    private Teacher() {
        super(Mastery.TEACHER, Items.LECTERN);
    }

    @Override
    public boolean watches() { return true; }

    @Override
    public String unlockMessage() { return "You are a Teacher now. You can teach anyone anything. You have nothing to teach."; }

    @Override
    public String masteryHundredMessage() { return "You are now a Master Teacher. You still can't teach anything."; }

    @Override
    public String awakenedMessage() { return "Class dismissed. Nobody learned anything."; }

    @Override
    public void onKey(ServerPlayer p, String ability) {
        LivingEntity target = Fx.lookTarget(p, 16);
        Fx.sound(p, PowerSounds.CHALK_TAP, 0.8f, 1f);
        whisper(p, target == null ? "You have nothing to teach." : "You try to teach " + target.getName().getString() + ". You have nothing to teach.");
    }

    @Override
    public void everySecond(ServerPlayer p) {
        Mastery.add(p, id, 1);
        stripOthers(p);
        if (questReady(p)) lesson(p);
    }

    // They quit, or something else took Teacher away: their old powers come back.
    @Override
    public void watchSecond(ServerPlayer p) {
        if (data(p).contains(PARKED)) bringBack(p);
    }

    @Override
    public void give(ServerPlayer p) {
        if (owns(p)) return;
        parkEverything(p);
        super.give(p);
    }

    private void stripOthers(ServerPlayer p) {
        int gone = SuperpowerUtil.removeSuperpowersByIds(p, other -> !other.equals(id));
        long now = p.level().getGameTime();
        if (gone > 0 && now - data(p).getLong(WARNED) > 6000) {
            data(p).putLong(WARNED, now);
            tell(p, "Teachers can't have other powers.");
        }
    }

    private void parkEverything(ServerPlayer p) {
        CompoundTag d = data(p);
        ListTag list = new ListTag();
        boolean hadHidden = false;
        for (ResourceLocation other : new ArrayList<>(SuperpowerUtil.getSuperpowerIds(p))) {
            if (other.equals(HiddenGrant.POWER)) hadHidden = true;
            else list.add(StringTag.valueOf(other.toString()));
        }
        if (hadHidden) {
            ListTag hidden = new ListTag();
            for (String h : PlayerHiddenData.get(p).owned()) hidden.add(StringTag.valueOf(h));
            d.put(PARKED_HIDDEN, hidden);
            HiddenGrant.wipe(p);
        }
        d.put(PARKED, list);
        CloneManager.dismissAll(p);
        SuperpowerUtil.removeAllSuperpowers(p);
    }

    private void bringBack(ServerPlayer p) {
        CompoundTag d = data(p);
        for (Tag t : d.getList(PARKED, Tag.TAG_STRING)) {
            ResourceLocation other = ResourceLocation.tryParse(t.getAsString());
            if (other != null) SuperpowerUtil.addSuperpower(p, other);
        }
        for (Tag t : d.getList(PARKED_HIDDEN, Tag.TAG_STRING)) HiddenGrant.give(p, t.getAsString());
        d.remove(PARKED);
        d.remove(PARKED_HIDDEN);
        Mastery.sync(p);
        tell(p, "You handed in your notice. Nobody noticed. Your old powers are back.");
    }

    // The quest: 3 whole Minecraft days at a lectern.
    private void lesson(ServerPlayer p) {
        CompoundTag d = data(p);
        ClassRule rule = new ClassRule(d.getInt(SECONDS), d.getInt(AWAY));
        ClassRule.Result result = rule.step(nearLectern(p));
        d.putInt(SECONDS, rule.seconds);
        d.putInt(AWAY, rule.away);
        switch (result) {
            case STARTED -> tell(p, "Class is in session. 3 Minecraft days to go. Don't wander off.");
            case NEW_DAY -> tell(p, "Day " + rule.seconds / ClassRule.SECONDS_PER_DAY + " of 3 is over. Nobody took notes.");
            case WALKED_OUT -> tell(p, "You walked out of class. Back to day 1.");
            case DONE -> {
                d.remove(SECONDS);
                d.remove(AWAY);
                p.level().playSound(null, p.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1f, 1f);
                awaken(p);
            }
            default -> {}
        }
    }

    private boolean nearLectern(ServerPlayer p) {
        CompoundTag d = data(p);
        BlockPos at = p.blockPosition();
        if (d.contains(LECTERN)) {
            BlockPos known = BlockPos.of(d.getLong(LECTERN));
            if (known.distManhattan(at) <= 3 && p.level().getBlockState(known).is(Blocks.LECTERN)) return true;
            d.remove(LECTERN);
        }
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-2, -1, -2), at.offset(2, 1, 2))) {
            if (p.level().getBlockState(pos).is(Blocks.LECTERN)) {
                d.putLong(LECTERN, pos.asLong());
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onTrade(TradeWithVillagerEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(e.getAbstractVillager() instanceof Villager v) || POWER.owns(p)) return;
        int level = v.getVillagerData().getLevel();
        if (!TeacherRules.tradeMakesMaster(level, v.getVillagerXp(), VillagerData.getMaxXpPerLevel(level))) return;
        if (v.getPersistentData().getBoolean(COUNTED)) return;
        v.getPersistentData().putBoolean(COUNTED, true);
        CompoundTag d = POWER.data(p);
        int masters = d.getInt(MASTERS) + 1;
        d.putInt(MASTERS, masters);
        if (masters == Setting.TEACHER_VILLAGERS.i()) {
            tell(p, "You've raised " + masters + " villagers to Master. A school has noticed. Crouch and right-click a lectern to become a Teacher. You'd have to give up every other power you have.");
        } else if (masters < Setting.TEACHER_VILLAGERS.i()) {
            whisper(p, "Another villager made Master. " + masters + "/" + Setting.TEACHER_VILLAGERS.i());
        }
    }

    // Crouch + right-click a lectern, twice: become a Teacher, or quit being one.
    @SubscribeEvent
    public static void onLectern(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isCrouching() || !p.level().getBlockState(e.getPos()).is(Blocks.LECTERN)) return;
        CompoundTag d = POWER.data(p);
        boolean owner = POWER.owns(p);
        if (!owner && d.getInt(MASTERS) < Setting.TEACHER_VILLAGERS.i()) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        long now = p.level().getGameTime();
        if (!TeacherRules.confirms(d.getLong(CLICK), now)) {
            d.putLong(CLICK, now);
            tell(p, owner ? "Quit teaching? Your old powers come back. Mastery stays. Do that again to quit."
                    : "Become a Teacher? All your other powers get put away until you quit. Do that again to sign.");
            return;
        }
        d.remove(CLICK);
        if (owner) SuperpowerUtil.removeSuperpower(p, POWER.id);
        else POWER.unlock(p);
    }
}
