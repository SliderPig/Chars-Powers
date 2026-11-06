package com.example.charpowers.cloning;

import com.example.charpowers.own.Setting;
import com.example.charpowers.CharPowers;
import com.example.charpowers.cloning.jobs.AttackJob;
import com.example.charpowers.cloning.jobs.RogueJob;
import com.example.charpowers.cloning.model.CloneRules;
import com.example.charpowers.hidden.HiddenConfig;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.sound.PowerSounds;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.fml.ModList;
import net.threetag.palladium.power.PowerManager;
import net.threetag.palladium.power.SuperpowerUtil;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Who has which clones out: summoning, dismissing, deaths, and everything a clone does once a second or so.
public final class CloneManager {
    private static final Map<UUID, CloneEntity[]> OUT = new HashMap<>();
    private static final String ROGUE_TEAM = "charclone_rogue";
    private static final int MAX_HOLD = 1200;

    private CloneManager() {}

    public static List<CloneEntity> clonesOf(UUID host) {
        CloneEntity[] slots = OUT.get(host);
        List<CloneEntity> list = new ArrayList<>();
        if (slots != null) for (CloneEntity c : slots) if (c != null) list.add(c);
        return list;
    }

    public static List<CloneEntity> all() {
        List<CloneEntity> list = new ArrayList<>();
        for (UUID host : OUT.keySet()) list.addAll(clonesOf(host));
        return list;
    }

    @Nullable
    public static CloneEntity clone(UUID host, int slot) {
        CloneEntity[] slots = OUT.get(host);
        return slots == null || slot < 0 || slot >= slots.length ? null : slots[slot];
    }

    @Nullable
    public static ServerPlayer host(CloneEntity c) {
        return c.getServer().getPlayerList().getPlayer(c.hostId);
    }

    private static int mastery(ServerPlayer host) {
        return Mastery.level(host, Mastery.CLONING);
    }

    // Pressing the Clone key: one clone, or every free slot once Dog Pile is unlocked. Only ones ticked in the Clones menu.
    public static void pressSummon(ServerPlayer host) {
        if (!ModList.get().isLoaded("curtain")) {
            tell(host, "Cloning needs the Curtain mod on the server.");
            return;
        }
        int level = mastery(host);
        boolean awakened = Mastery.isAwakened(host, Mastery.CLONING);
        int want = CloneRules.toSummon(level, awakened, clonesOf(host.getUUID()).size());
        if (want == 0) {
            tell(host, "You can't have any more clones out.");
            return;
        }
        boolean danger = underAttack(host);
        long now = host.level().getGameTime();
        boolean anyPicked = false;
        for (int slot = 0; slot < CloneRules.cap(level, awakened) && want > 0; slot++) {
            if (clone(host.getUUID(), slot) != null || !CloneData.picked(host, slot)) continue;
            anyPicked = true;
            long lockedFor = CloneData.lockedUntil(host, slot) - now;
            if (lockedFor > 0) {
                tell(host, CloneData.label(host, slot) + " can't come back for " + minutes(lockedFor) + " more.");
                continue;
            }
            if (all().size() >= HiddenConfig.MAX_CLONES_ON_SERVER.get()) {
                tell(host, "The server has as many clones as it can take right now.");
                return;
            }
            if (summonOne(host, slot, danger)) want--;
        }
        if (!anyPicked) tell(host, "None of the clones you've ticked are free. Tick them in the Clones menu.");
    }

    // Goes over its head on everyone's PC (client/CloneClient reads it from there).
    static void showNickname(CloneEntity c, ServerPlayer host) {
        String name = CloneData.nickname(host, c.slot);
        c.setCustomName(name.isEmpty() ? null : Component.literal(name));
    }

    private static String minutes(long ticks) {
        long m = Math.max(1, ticks / 1200);
        return m + (m == 1 ? " minute" : " minutes");
    }

    private static void tell(ServerPlayer p, String text) {
        p.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // Hurt in the last 5 seconds, or a mob nearby has you as its target.
    public static boolean underAttack(ServerPlayer host) {
        if (host.getLastHurtByMob() != null && host.tickCount - host.getLastHurtByMobTimestamp() <= 100) return true;
        if (host.hurtTime > 0) return true;
        AABB near = host.getBoundingBox().inflate(24);
        return !host.level().getEntitiesOfClass(Mob.class, near, m -> m.getTarget() == host).isEmpty();
    }

    private static boolean summonOne(ServerPlayer host, int slot, boolean danger) {
        MinecraftServer server = host.getServer();
        ServerLevel level = host.serverLevel();
        // Two players can't share an ID, so each slot has its own, and a hidden name so commands aimed at the
        // player can never hit the clone. Everyone's PC shows the player's real name (client/CloneClient).
        // The dot matters: real names can't have one, but commands can still read it. Other mods' scripts aim commands
        // at a player's name (Echoes gives its powers that way), and a ~ here made those commands quietly fail.
        UUID id = UUID.nameUUIDFromBytes(("charclone:" + host.getUUID() + ":" + slot).getBytes(StandardCharsets.UTF_8));
        String name = host.getGameProfile().getName();
        if (name.length() > 13) name = name.substring(0, 13);
        GameProfile profile = new GameProfile(id, name + ".c" + (slot + 1));
        profile.getProperties().putAll(host.getGameProfile().getProperties());
        try {
            // always a fresh body: never load what an earlier clone in this slot had
            Files.deleteIfExists(server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(id + ".dat"));
        } catch (IOException e) {
            CharPowers.LOG.warn("Couldn't clear the old data of clone {}", id, e);
        }
        CloneEntity c;
        try {
            c = new CloneEntity(server, level, profile, host.getUUID(), slot);
            OUT.computeIfAbsent(host.getUUID(), k -> new CloneEntity[CloneRules.SLOTS])[slot] = c;
            server.getPlayerList().placeNewPlayer(CloneConnection.make(), c);
        } catch (Exception e) {
            CharPowers.LOG.error("Couldn't make a clone for {}", host.getGameProfile().getName(), e);
            OUT.get(host.getUUID())[slot] = null;
            tell(host, "The clone couldn't form. (The server log says why.)");
            return false;
        }
        Vec3 spot = spotNear(host);
        c.teleportTo(level, spot.x, spot.y, spot.z, host.getYRot(), host.getXRot());
        c.setGameMode(GameType.SURVIVAL);
        boolean awakened = Mastery.isAwakened(host, Mastery.CLONING);
        c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(CloneRules.maxHealth(mastery(host), awakened));
        if (awakened) c.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(c.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() + Setting.CLONE_AWAKENED_DAMAGE.d());
        c.setHealth(c.getMaxHealth());
        c.fixBody();
        showNickname(c, host);
        hideFromTab(server, List.of(c), null);
        PlayerTeam team = host.getTeam() instanceof PlayerTeam t ? t : null;
        if (team != null) server.getScoreboard().addPlayerToTeam(c.getScoreboardName(), team);
        CloneGear.refresh(c, host, true);
        poof(c);
        com.example.charpowers.anim.AnimNet.play(host, "split");
        if (danger) c.setJob(new AttackJob(host.blockPosition()));
        return true;
    }

    static Vec3 spotNear(ServerPlayer host) {
        for (int i = 0; i < 8; i++) {
            double a = host.getRandom().nextDouble() * Math.PI * 2;
            Vec3 p = host.position().add(Math.cos(a) * 1.5, 0, Math.sin(a) * 1.5);
            if (host.level().noCollision(host, host.getBoundingBox().move(p.subtract(host.position())))) return p;
        }
        return host.position();
    }

    private static void poof(CloneEntity c) {
        c.serverLevel().sendParticles(ParticleTypes.POOF, c.getX(), c.getY() + 1, c.getZ(), 15, 0.3, 0.5, 0.3, 0.02);
        c.level().playSound(null, c.blockPosition(), PowerSounds.MIRROR_SHIMMER.get(), SoundSource.PLAYERS, 0.7f, 1f);
    }

    // TAB shows nobody extra: the clone stays known to everyone's PC (for its skin), just not listed.
    static void hideFromTab(MinecraftServer server, Collection<CloneEntity> clones, @Nullable ServerPlayer onlyTo) {
        if (clones.isEmpty()) return;
        FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
        b.writeEnumSet(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED), ClientboundPlayerInfoUpdatePacket.Action.class);
        b.writeCollection(clones, (buf, c) -> {
            buf.writeUUID(c.getUUID());
            buf.writeBoolean(false);
        });
        ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(b);
        if (onlyTo != null) onlyTo.connection.send(packet);
        else server.getPlayerList().broadcastAll(packet);
    }

    private static void takeOffTable(CloneEntity c) {
        CloneEntity[] slots = OUT.get(c.hostId);
        if (slots != null && slots[c.slot] == c) slots[c.slot] = null;
        Scoreboard board = c.getServer().getScoreboard();
        if (board.getPlayersTeam(c.getScoreboardName()) != null) board.removePlayerFromTeam(c.getScoreboardName());
    }

    // Unsummoning: the gear goes home, and Mastery Share hands over all the XP and mastery it gathered.
    public static void dismiss(CloneEntity c) {
        if (c.gone) return;
        ServerPlayer host = host(c);
        if (host != null) {
            host.giveExperiencePoints(c.totalExperience);
            Mastery.add(host, Mastery.CLONING, c.earnedMastery);
        }
        CloneGear.giveBack(c, host);
        poof(c);
        takeOffTable(c);
        c.leave(Component.literal("Dismissed"));
    }

    // Everything standard goes home (Dog Pile, and the player leaving). A rogue clone can't be sent home.
    public static void dismissAll(ServerPlayer host) {
        for (CloneEntity c : clonesOf(host.getUUID())) if (!c.rogue) dismiss(c);
    }

    // What the clones menu shows: every slot you've unlocked.
    public static List<CloneInfo> describe(ServerPlayer host) {
        int level = mastery(host);
        long now = host.level().getGameTime();
        List<CloneInfo> list = new ArrayList<>();
        for (int slot = 0; slot < CloneRules.cap(level, Mastery.isAwakened(host, Mastery.CLONING)); slot++) {
            CloneEntity c = clone(host.getUUID(), slot);
            GlobalPos chest = CloneData.chest(host, slot);
            String chestText = chest == null ? "None marked" : chest.pos().toShortString();
            String name = CloneData.nickname(host, slot);
            boolean picked = CloneData.picked(host, slot);
            if (c == null) {
                long lockedFor = CloneData.lockedUntil(host, slot) - now;
                String state = lockedFor > 0 ? "Back in " + minutes(lockedFor) : "Not out";
                list.add(new CloneInfo(slot, name, picked, false, state, "", 0, 0, 0, "", chestText, List.of(), List.of()));
                continue;
            }
            List<Component> powers = new ArrayList<>();
            PowerManager.getPowerHandler(c).ifPresent(h -> h.getPowerHolders().values().forEach(p -> {
                if (!c.rolledPowers.contains(p.getPower().getId())) powers.add(p.getPower().getName());
            }));
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < 41; i++) items.add(c.getInventory().getItem(i).copy());
            String where = c.blockPosition().toShortString() + " (" + c.level().dimension().location().getPath() + ")";
            if (c.level() == host.level()) where += ", " + Math.round(Math.sqrt(c.distanceToSqr(host))) + " blocks away";
            list.add(new CloneInfo(slot, name, picked, true, c.rogue ? "Rogue!" : c.job.id(), c.job.describe(c), c.getHealth(), c.getMaxHealth(),
                    c.getFoodData().getFoodLevel(), where, chestText, powers, items));
        }
        return list;
    }

    static void onDied(CloneEntity c) {
        ServerPlayer host = host(c);
        if (host != null) {
            long ticks = CloneRules.lockTicks(mastery(host));
            CloneData.lock(host, c.slot, host.level().getGameTime() + ticks);
            tell(host, (c.rogue ? "Your rogue clone is dead." : CloneData.label(host, c.slot) + " died.") + " It can't come back for " + minutes(ticks) + ".");
            host.level().playSound(null, c.blockPosition(), PowerSounds.GLASS_SHATTER.get(), SoundSource.PLAYERS, 0.8f, 1f);
        }
        takeOffTable(c);
        c.leave(Component.literal("Died"));
    }

    // The player left the game: nothing is left behind. A rogue fight ends as if the clone had died.
    public static void hostLeft(ServerPlayer host) {
        endRogues(host);
        dismissAll(host);
        OUT.remove(host.getUUID());
    }

    // A rogue clone fights until you or it dies.
    public static void endRogues(ServerPlayer host) {
        for (CloneEntity c : clonesOf(host.getUUID())) {
            if (!c.rogue) continue;
            CloneData.lock(host, c.slot, host.level().getGameTime() + CloneRules.lockTicks(mastery(host)));
            CloneGear.giveBack(c, host);
            poof(c);
            takeOffTable(c);
            c.leave(Component.literal("The fight is over"));
        }
    }

    static void tickClone(CloneEntity c) {
        ServerPlayer host = host(c);
        if (host == null) {
            takeOffTable(c);
            c.leave(Component.literal("Host left"));
            return;
        }
        if (c.tickCount % 20 == 0) {
            if (!c.rogue) c.earnedMastery++;
            stripRolledPowers(c);
            CloneGear.eatIfHungry(c, host);
        }
        if (c.tickCount % 100 == 0) {
            CloneGear.refresh(c, host, false);
            if (!c.rogue && mastery(host) >= CloneRules.LEVEL_POWER_CLONE && c.tryingItemFor == 0) CloneGear.tryPowerItem(c, host);
        }
        if (c.tryingItemFor > 0) {
            if (c.isUsingItem()) {
                // still holding the button down; a minute is plenty, and a bow would otherwise be held for ever
                if (++c.tryHeld >= MAX_HOLD) c.releaseUsingItem();
            } else if (--c.tryingItemFor == 0) {
                finishPowerTry(c, host);
            }
        }
        c.job.tick(c);
        c.brain.tick();
        c.mover.tick();
    }

    // Other mods roll powers on anyone who joins. A clone only keeps what Power Clone gave it.
    private static void stripRolledPowers(CloneEntity c) {
        if (c.tryingItemFor > 0) return;
        SuperpowerUtil.removeSuperpowersByIds(c, id -> {
            if (c.allowedPowers.contains(id)) return false;
            c.rolledPowers.add(id);
            return true;
        });
    }

    static Set<ResourceLocation> powersOf(CloneEntity c) {
        return PowerManager.getPowerHandler(c).map(h -> (Set<ResourceLocation>) new HashSet<>(h.getPowerHolders().keySet())).orElseGet(HashSet::new);
    }

    private static void finishPowerTry(CloneEntity c, ServerPlayer host) {
        Set<ResourceLocation> gained = powersOf(c);
        gained.removeAll(c.powersBefore);
        gained.removeAll(c.rolledPowers);
        c.allowedPowers.addAll(gained);
        // whatever is left of the item goes home, unless the power comes from holding it
        if (gained.isEmpty() || c.tryingHand != InteractionHand.OFF_HAND) CloneGear.returnTried(c, host);
        c.tryingHand = null;
        if (gained.isEmpty()) return;
        tell(host, CloneData.label(host, c.slot) + " gained a power from its chest.");
        CloneBrain.learnSkills(c);
        if (!c.rogueChecked) {
            c.rogueChecked = true;
            if (CloneRules.rollsRogue(mastery(host), c.getRandom()::nextDouble)) goRogue(c, host);
        }
    }

    // There's no goal system on a player to add NearestAttackableTargetGoal to, so RogueJob does its work by hand:
    // follower behaviour gone, its own team so team rules no longer protect you, and you as the only target.
    private static void goRogue(CloneEntity c, ServerPlayer host) {
        c.rogue = true;
        Scoreboard board = c.getServer().getScoreboard();
        PlayerTeam team = board.getPlayerTeam(ROGUE_TEAM);
        if (team == null) team = board.addPlayerTeam(ROGUE_TEAM);
        board.addPlayerToTeam(c.getScoreboardName(), team);
        c.setJob(new RogueJob());
        host.sendSystemMessage(Component.literal(CloneData.label(host, c.slot) + " looks at you. It isn't you any more.").withStyle(ChatFormatting.RED));
        host.level().playSound(null, c.blockPosition(), SoundEvents.ENDERMAN_STARE, SoundSource.PLAYERS, 1f, 0.8f);
    }
}
