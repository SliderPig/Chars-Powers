package com.example.charpowers.smoke;

import com.example.charpowers.CharPowers;
import com.example.charpowers.armour.InvincibleArmour;
import com.example.charpowers.armour.SuitItems;
import com.example.charpowers.book.BookKeeping;
import com.example.charpowers.futuresight.FutureSight;
import com.example.charpowers.game.GamePlayer;
import com.example.charpowers.lantern.GreenLantern;
import com.example.charpowers.mastery.Mastery;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

// gradlew runClient -Psmoke: opens the real game, makes a flat world, goes through everything you can see, takes a screenshot
// of each (run-client/screenshots) and quits. Only compiled for that run, never in the jar.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class ClientChecks {
    private static final boolean ON = Boolean.getBoolean("charpowers.smokeclient");
    private static final boolean PUNCHY = Boolean.getBoolean("charpowers.punchycheck");
    private static final boolean LAB = Boolean.getBoolean("charpowers.punchylab");
    static final boolean LANTERN_LAB = Boolean.getBoolean("charpowers.lanternlab");
    private static final boolean STEAL = Boolean.getBoolean("charpowers.stealcheck");
    private static final boolean HOUSES = Boolean.getBoolean("charpowers.housecheck");
    private static final boolean SMALL = Boolean.getBoolean("charpowers.smallcheck");
    private static final boolean PETS = Boolean.getBoolean("charpowers.petcheck");
    private static final boolean GUI = Boolean.getBoolean("charpowers.guicheck");
    private static final boolean GUI_MODS = Boolean.getBoolean("charpowers.guimods");
    private static final boolean FEATURES = Boolean.getBoolean("charpowers.featurecheck");
    private static final boolean ICONS = Boolean.getBoolean("charpowers.iconcheck");
    private static final boolean LAN_HOST = Boolean.getBoolean("charpowers.lanhost"), LAN_GUEST = Boolean.getBoolean("charpowers.languest");
    private static final int LAN_PORT = 25599;
    private static boolean guestSeen;
    private static int guestTicks = -1;
    private static boolean labReady;
    private static final String[] WEAPONS = {"charpowers:knight_killer", "charpowers:crimson_edge", "charpowers:gravity_blade", "charpowers:shadow_scythe", "charpowers:grand_spear", "charpowers:cocytus_hammer", "minecraft:iron_sword", "minecraft:iron_axe"};
    private static final Deque<Step> STEPS = new ArrayDeque<>();
    private static boolean titleSeen, created, scripted;
    private static int wait;

    private record Step(int after, Runnable what) {}

    private ClientChecks() {}

    static void later(int ticks, Runnable what) {
        STEPS.add(new Step(ticks, what));
    }

    private static final boolean LINEUP = Boolean.getBoolean("charpowers.lineupcheck");
    private static net.minecraft.world.phys.Vec3 anchor;

    // python tools/run_real_client.py pack lineup: on its own, in a flat bright daytime overworld (never in a gate), no windows or notices. Every gate
    // creature in rows of six, then the bosses two at a time, each row photographed from a three-quarter angle with the names showing.
    private static void lineupScript() {
        later(20, () -> {
            Minecraft.getInstance().options.hideGui = false; // names only show with the hud on
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
        });
        server(30, p -> {
            ServerLevel level = p.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            level.getServer().setDifficulty(Difficulty.NORMAL, true);
            p.setGameMode(GameType.CREATIVE);
            p.getAbilities().flying = false;
            anchor = p.position();
        });
        int mobGroups = (int) java.util.Arrays.stream(com.example.charpowers.game.mob.GateKind.values()).filter(k -> !k.boss).count() / 6;
        int bossGroups = (int) java.util.Arrays.stream(com.example.charpowers.game.mob.GateKind.values()).filter(k -> k.boss).count() / 2;
        for (int g = 0; g < mobGroups; g++) {
            int group = g;
            server(15, p -> lineup(p, false, group));
            shot(40, "L%02d_mobs".formatted(g));
        }
        for (int g = 0; g < bossGroups; g++) {
            int group = g;
            server(15, p -> lineup(p, true, group));
            shot(40, "L%02d_bosses".formatted(10 + g));
        }
        later(5, () -> {
            CharPowers.LOG.info("[ClientCheck] finished");
            Minecraft.getInstance().stop();
        });
    }

    private static void lineup(ServerPlayer p, boolean bosses, int group) {
        ServerLevel lv = p.serverLevel();
        lv.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new net.minecraft.world.phys.AABB(anchor, anchor).inflate(80),
                m -> m instanceof com.example.charpowers.game.mob.GateMob || m instanceof com.example.charpowers.game.mob.GateBoss).forEach(net.minecraft.world.entity.Entity::discard);
        double dist = bosses ? 8 : 9, side = bosses ? 3 : 3, gap = bosses ? 6 : 2.4;
        // the camera stands off to one side and looks at the middle of the row, so everything is seen at about three quarters
        double yaw = -Math.toDegrees(Math.atan2(dist, side));
        p.connection.teleport(anchor.x, anchor.y, anchor.z - side, (float) yaw, bosses ? 6f : 10f);
        java.util.List<com.example.charpowers.game.mob.GateKind> picks = new java.util.ArrayList<>();
        for (com.example.charpowers.game.mob.GateKind k : com.example.charpowers.game.mob.GateKind.values()) {
            if (k.boss == bosses) picks.add(k);
        }
        int per = bosses ? 2 : 6, from = group * per, to = Math.min(picks.size(), from + per);
        for (int i = from; i < to; i++) {
            var kind = picks.get(i);
            double z = anchor.z + (i - from - (to - from - 1) / 2.0) * gap;
            net.minecraft.world.entity.Mob m;
            if (bosses) {
                m = com.example.charpowers.game.mob.GateBoss.spawn(lv, kind, new net.minecraft.world.phys.Vec3(anchor.x + dist, anchor.y, z), 3, 1, x -> {}, () -> {});
            } else {
                m = (net.minecraft.world.entity.Mob) com.example.charpowers.game.mob.GateMobs.mobType(kind).create(lv);
                boolean flier = kind == com.example.charpowers.game.mob.GateKind.IMP || kind == com.example.charpowers.game.mob.GateKind.WRAITH || kind == com.example.charpowers.game.mob.GateKind.SENTRY_DRONE;
                m.moveTo(anchor.x + dist, anchor.y + (flier ? 0.8 : 0), z, 90f, 0f);
                m.finalizeSpawn(lv, lv.getCurrentDifficultyAt(p.blockPosition()), net.minecraft.world.entity.MobSpawnType.SPAWN_EGG, null, null);
                lv.addFreshEntity(m);
            }
            m.setCustomName(net.minecraft.network.chat.Component.literal(kind.title));
            m.setCustomNameVisible(true);
            m.setYRot(90f);
            m.setYBodyRot(90f);
            m.setYHeadRot(90f);
            m.setNoAi(true);
            m.setInvulnerable(true);
            m.setNoGravity(false);
        }
    }

    static void shot(int ticks, String name) {
        later(ticks, () -> Screenshot.grab(Minecraft.getInstance().gameDirectory, name + ".png", Minecraft.getInstance().getMainRenderTarget(), c -> {}));
    }

    private static void server(int ticks, Consumer<ServerPlayer> what) {
        later(ticks, () -> {
            var server = Minecraft.getInstance().getSingleplayerServer();
            server.execute(() -> {
                try {
                    what.accept(server.getPlayerList().getPlayers().get(0));
                } catch (Throwable t) {
                    CharPowers.LOG.error("[ClientCheck] step failed", t);
                }
            });
        });
    }

    private static void camera(int ticks, CameraType type) {
        later(ticks, () -> Minecraft.getInstance().options.setCameraType(type));
    }

    private static void key(int ticks, String ability) {
        server(ticks, p -> GreenLantern.POWER.onKey(p, ability));
    }

    private static void script() {
        server(40, p -> {
            ServerLevel level = p.serverLevel();
            p.setGameMode(GameType.CREATIVE);
            p.getAbilities().flying = false;
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            p.setXRot(15f);
            p.setYRot(-90f);
            p.connection.teleport(p.getX(), p.getY(), p.getZ(), -90f, 15f);
        });
        // Invincible Armour in lime and silver, and its station
        server(20, p -> {
            InvincibleArmour.POWER.give(p);
            p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(SuitItems.SUIT.get()));
            p.serverLevel().setBlock(p.blockPosition().offset(3, 0, 2), SuitItems.STATION.get().defaultBlockState(), 2);
            p.getInventory().setItem(0, new ItemStack(SuitItems.SUIT.get()));
            p.getInventory().setItem(1, new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("charpowers", "suit_head_part"))));
            p.getInventory().setItem(2, new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("charpowers", "strength_module"))));
            p.getInventory().setItem(3, new ItemStack(SuitItems.POWER_BATTERY.get()));
            p.getInventory().setItem(4, new ItemStack(SuitItems.THE_BOOK.get()));
            p.getInventory().setItem(5, new ItemStack(SuitItems.STATION.get()));
            p.getInventory().setItem(6, new ItemStack(SuitItems.POWER_SYRINGE.get()));
            ItemStack full = new ItemStack(SuitItems.POWER_SYRINGE.get());
            full.getOrCreateTag().putString("stored", "charpowers:cloning");
            p.getInventory().setItem(7, full);
        });
        camera(5, CameraType.THIRD_PERSON_FRONT);
        shot(30, "01_suit_front");
        camera(1, CameraType.THIRD_PERSON_BACK);
        shot(15, "02_suit_back");
        server(1, p -> {
            BlockPos at = p.blockPosition().offset(-2, 0, 0);
            p.serverLevel().setBlock(at, SuitItems.STATION.get().defaultBlockState(), 2);
            p.serverLevel().setBlock(at.offset(0, 0, 2), SuitItems.STATION.get().defaultBlockState().setValue(com.example.charpowers.armour.ChargingStation.CHARGING, true), 2);
            p.connection.teleport(p.getX(), p.getY(), p.getZ(), 90f, 20f);
        });
        camera(1, CameraType.FIRST_PERSON);
        shot(15, "02b_station_close");
        server(1, p -> p.connection.teleport(p.getX(), p.getY(), p.getZ(), -90f, 15f));
        camera(1, CameraType.FIRST_PERSON);
        shot(10, "03_suit_first_person");
        // the suit rising up behind you, opening, closing round you and dropping its helmet on (seen from behind), then going away (from the front)
        server(1, p -> {
            p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            p.getInventory().clearContent();
            p.getInventory().setItem(0, new ItemStack(SuitItems.SUIT.get()));
            p.connection.teleport(p.getX(), p.getY(), p.getZ(), 0f, 4f);
        });
        later(1, () -> com.example.charpowers.armour.client.VisorSettings.preset("standard"));
        camera(1, CameraType.THIRD_PERSON_BACK);
        server(2, p -> InvincibleArmour.POWER.onKey(p, "call_suit"));
        shot(14, "04a_equip_flying");
        shot(14, "04b_equip_arriving");
        shot(12, "04c_equip_opening");
        shot(9, "04d_equip_open");
        shot(8, "04e_equip_closing");
        shot(8, "04f_equip_arms");
        shot(7, "04g_equip_helmet_lands");
        shot(30, "04h_equip_worn");
        camera(1, CameraType.THIRD_PERSON_FRONT);
        server(1, p -> {
            p.setShiftKeyDown(true);
            InvincibleArmour.POWER.onKey(p, "call_suit");
        });
        shot(7, "05a_leave_helmet");
        shot(14, "05b_leave_opening");
        shot(16, "05c_leave_open");
        shot(16, "05d_leave_closing");
        server(70, p -> p.setShiftKeyDown(false));
        // the same from inside: first person, with the visor starting up
        server(1, p -> p.connection.teleport(p.getX(), p.getY(), p.getZ(), 0f, 4f));
        camera(1, CameraType.FIRST_PERSON);
        server(2, p -> InvincibleArmour.POWER.onKey(p, "call_suit"));
        shot(36, "06a_fp_hover");
        shot(10, "06b_fp_open");
        shot(10, "06c_fp_closing");
        shot(8, "06d_fp_arms");
        shot(6, "06e_fp_visor");
        shot(6, "06f_fp_boot");
        shot(14, "06g_fp_boot_done");
        // the visor at GUI scale 2 and 3, with something to look at
        server(1, p -> {
            p.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
            net.minecraft.world.entity.monster.Zombie z = net.minecraft.world.entity.EntityType.ZOMBIE.create(p.serverLevel());
            z.moveTo(p.getX(), p.getY(), p.getZ() + 7, 180f, 0f);
            z.setNoAi(true);
            z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
            z.setPersistenceRequired();
            p.serverLevel().addFreshEntity(z);
            net.minecraft.world.entity.monster.Skeleton k = net.minecraft.world.entity.EntityType.SKELETON.create(p.serverLevel());
            k.moveTo(p.getX() + 6, p.getY(), p.getZ() + 4, 180f, 0f);
            k.setNoAi(true);
            k.setItemSlot(EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
            k.setPersistenceRequired();
            p.serverLevel().addFreshEntity(k);
            p.hurt(p.damageSources().magic(), 1f);
        });
        size(2, 1600, 900, 2);
        shot(14, "07a_visor_scale2");
        later(1, () -> com.example.charpowers.armour.client.VisorSettings.preset("full"));
        later(1, () -> com.example.charpowers.armour.client.SuitVisor.station("minecraft:overworld", net.minecraft.core.BlockPos.asLong(160, -60, 40)));
        size(2, 1600, 900, 3);
        shot(10, "07b_visor_scale3_full");
        later(1, () -> {
            var me = Minecraft.getInstance().player;
            me.hurtDuration = 10;
            me.hurtTime = 10;
        });
        shot(1, "07e_hit_flash");
        later(1, () -> com.example.charpowers.armour.client.VisorSettings.preset("minimal"));
        shot(6, "07c_visor_minimal");
        later(1, () -> com.example.charpowers.armour.client.VisorSettings.preset("standard"));
        size(2, 1600, 900, 2);
        later(2, () -> Minecraft.getInstance().setScreen(new com.example.charpowers.armour.client.VisorScreen()));
        shot(14, "07d_visor_settings");
        later(1, () -> Minecraft.getInstance().setScreen(null));
        server(2, p -> {
            p.getInventory().clearContent();
            p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, p.getBoundingBox().inflate(20)).forEach(net.minecraft.world.entity.Entity::discard);
            p.getServer().setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true);
        });
        // Green Lantern
        server(1, p -> {
            InvincibleArmour.POWER.data(p).remove("x");
            p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            p.getInventory().clearContent();
            GreenLantern.POWER.give(p);
        });
        camera(5, CameraType.THIRD_PERSON_FRONT);
        shot(20, "04_lantern_front");
        camera(1, CameraType.THIRD_PERSON_BACK);
        shot(15, "05_lantern_back");
        camera(1, CameraType.FIRST_PERSON);
        shot(10, "06_lantern_arm");
        camera(1, CameraType.THIRD_PERSON_BACK);
        server(5, p -> {
            Pig pig = new Pig(EntityType.PIG, p.level());
            pig.moveTo(p.getX() - 8, p.getY(), p.getZ(), 0f, 0f);
            pig.setNoAi(true);
            pig.setInvulnerable(true);
            p.level().addFreshEntity(pig);
            p.connection.teleport(p.getX(), p.getY(), p.getZ(), 90f, 10f);
        });
        camera(1, CameraType.THIRD_PERSON_FRONT);
        server(5, ClientChecks::pigInFront);
        key(10, "rising_fist");
        shot(14, "07_fist");
        server(10, ClientChecks::pigInFront);
        key(10, "hand_slam");
        shot(15, "08_slam");
        shot(3, "08b_slam_hit");
        server(20, ClientChecks::pigInFront);
        key(5, "backhand_slap");
        shot(17, "09_slap");
        shot(4, "09b_slap_hit");
        server(20, ClientChecks::pigInFront);
        key(5, "hand_throw");
        shot(12, "10_grab");
        server(30, p -> GreenLantern.POWER.onKeyUp(p, "hand_throw"));
        shot(6, "11_throw");
        camera(1, CameraType.THIRD_PERSON_BACK);
        key(30, "construct_sword");
        shot(20, "12_sword");
        server(1, p -> GreenLantern.POWER.onClientAction(p, "swing"));
        shot(5, "13_sword_swing");
        key(25, "construct_sword");
        key(15, "launch_swords");
        shot(40, "14_swords_forming");
        server(1, p -> GreenLantern.POWER.onKeyUp(p, "launch_swords"));
        shot(4, "15_swords_flying");
        key(25, "minigun");
        shot(12, "16_minigun");
        server(1, p -> GreenLantern.POWER.onClientAction(p, "fire:1"));
        shot(20, "17_minigun_firing");
        server(1, p -> {
            GreenLantern.POWER.onClientAction(p, "fire:0");
            GreenLantern.POWER.onKey(p, "minigun");
        });
        server(15, p -> GreenLantern.POWER.data(p).putDouble("battery", 100));
        key(5, "turret");
        server(15, p -> {
            Zombie zombie = new Zombie(EntityType.ZOMBIE, p.level());
            zombie.moveTo(p.getX() + 8, p.getY(), p.getZ() + 2, 0f, 0f);
            zombie.setNoAi(true);
            zombie.setPersistenceRequired();
            p.level().addFreshEntity(zombie);
        });
        shot(30, "18_turret_shooting");
        server(5, p -> GreenLantern.POWER.data(p).putDouble("battery", 100));
        key(1, "bubble");
        shot(20, "19_bubble");
        key(5, "bubble");
        server(1, p -> p.getInventory().clearContent());
        later(1, () -> Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON));
        key(5, "minigun");
        shot(15, "19b_minigun_first_person");
        key(1, "minigun");
        later(1, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK));
        // Sinestro
        server(10, p -> {
            GreenLantern.POWER.data(p).putDouble("battery", 100);
            Mastery.setLevel(p, GreenLantern.POWER.id, 100);
            p.serverLevel().setDayTime(18000);
        });
        server(15, p -> {
            p.serverLevel().setDayTime(18000);
            GreenLantern.POWER.everySecond(p);
            p.serverLevel().setDayTime(6000);
        });
        camera(1, CameraType.THIRD_PERSON_BACK);
        shot(50, "20_sinestro");
        shot(70, "21_sinestro_attacking");
        // Book Keeping and Game Player's screens
        server(5, p -> {
            BookKeeping.POWER.give(p);
            BookKeeping.POWER.write(p, 0);
            BookKeeping.POWER.open(p);
        });
        shot(15, "22_book");
        later(1, () -> Minecraft.getInstance().setScreen(null));
        camera(1, CameraType.FIRST_PERSON);
        server(1, p -> {
            GreenLantern.POWER.data(p).putDouble("battery", 100);
            p.connection.teleport(p.getX(), p.getY(), p.getZ(), 90f, 0f);
        });
        server(5, p -> GamePlayer.POWER.trigger(p));
        server(10, p -> GamePlayer.POWER.watchSecond(p));
        shot(12, "22a_gate");
        server(1, p -> {
            BlockPos gate = BlockPos.of(GamePlayer.POWER.data(p).getLong("gate"));
            p.connection.teleport(gate.getX() + 0.5, gate.getY(), gate.getZ() + 0.5, 90f, 0f);
        });
        server(5, p -> GamePlayer.POWER.watchSecond(p));
        shot(25, "22b_altar_room");
        server(1, p -> {
            BlockPos altar = BlockPos.of(GamePlayer.POWER.data(p).getLong("altar"));
            p.connection.teleport(altar.getX() + 0.5, altar.getY() + 1, altar.getZ() + 0.5, 0f, 20f);
            p.setShiftKeyDown(true);
            p.setHealth(1f);
            GamePlayer.POWER.watchSecond(p);
            p.setShiftKeyDown(false);
        });
        shot(25, "22c_welcome_player");
        server(5, p -> {
            GamePlayer.POWER.trigger(p);
            GamePlayer.POWER.send(p, true);
        });
        shot(15, "23_game_blue_status");
        later(1, () -> setTab(1));
        shot(5, "24_game_blue_quests");
        later(1, () -> setTab(2));
        shot(5, "25_game_blue_skills");
        server(1, p -> GamePlayer.POWER.onClientAction(p, "look"));
        shot(8, "26_game_plain_skills");
        later(1, () -> setTab(0));
        shot(5, "27_game_plain_status");
        server(1, p -> GamePlayer.POWER.onClientAction(p, "look"));
        later(6, () -> Minecraft.getInstance().setScreen(null));
        server(1, p -> {
            p.setGameMode(GameType.SURVIVAL);
            p.getInventory().add(new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
            p.getInventory().add(new ItemStack(net.minecraft.world.item.Items.GOLDEN_APPLE, 12));
            p.getInventory().add(new ItemStack(net.minecraft.world.item.Items.TORCH, 40));
            p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.DIAMOND_HELMET));
            p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE));
            p.setItemSlot(EquipmentSlot.FEET, new ItemStack(net.minecraft.world.item.Items.IRON_BOOTS));
            p.connection.teleport(p.getX(), p.getY(), p.getZ(), 90f, 5f);
        });
        later(8, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        });
        shot(12, "27x_inventory");
        later(1, () -> holoPage("EQUIPMENT"));
        shot(8, "27y_equipment");
        later(1, () -> Minecraft.getInstance().setScreen(null));
        server(1, p -> GamePlayer.POWER.openStorage(p));
        shot(12, "27z_storage");
        later(1, () -> Minecraft.getInstance().player.closeContainer());
        server(1, p -> {
            p.setGameMode(GameType.CREATIVE);
            p.getInventory().clearContent();
            GamePlayer.POWER.send(p, true);
        });
        server(1, p -> GamePlayer.POWER.awaken(p));
        later(8, () -> setTab(4));
        server(1, p -> {
            GamePlayer.POWER.data(p).putInt("coins", 30);
            GamePlayer.POWER.send(p, false);
        });
        shot(8, "27b_game_shop");
        later(1, () -> Minecraft.getInstance().setScreen(null));
        shot(20, "27c_notification");
        // a gate on command, and inside it
        server(5, p -> {
            // gates are empty on Peaceful, and mobs leave a creative player alone
            p.getServer().setDifficulty(Difficulty.NORMAL, true);
            p.setGameMode(GameType.SURVIVAL);
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 1200, 4, false, false));
            GamePlayer.POWER.data(p).remove("openWait");
            Mastery.setLevel(p, GamePlayer.POWER.id, 100);
            p.connection.teleport(p.getX(), p.getY(), p.getZ(), 90f, 0f);
            GamePlayer.POWER.onClientAction(p, "gate:5");
            GamePlayer.POWER.send(p, true);
        });
        later(5, () -> setTab(3));
        shot(8, "27g_gates_tab");
        later(1, () -> Minecraft.getInstance().setScreen(null));
        server(5, p -> {
            var gates = GamePlayer.POWER.data(p).getList("gates", net.minecraft.nbt.Tag.TAG_COMPOUND);
            BlockPos gate = BlockPos.of(gates.getCompound(gates.size() - 1).getLong("pos"));
            p.connection.teleport(gate.getX() + 0.5, gate.getY(), gate.getZ() + 0.5, 90f, 0f);
        });
        shot(50, "27h_gate_inside");
        shot(60, "27i_gate_fight");
        server(5, p -> {
            com.example.charpowers.own.Pocket.leave(p);
            p.getServer().setDifficulty(Difficulty.PEACEFUL, true);
            p.setGameMode(GameType.CREATIVE);
            p.removeAllEffects();
            int slot = 0;
            for (com.example.charpowers.game.SystemWeapon.Kind kind : com.example.charpowers.game.SystemWeapon.Kind.values()) {
                p.getInventory().setItem(slot++, new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("charpowers", kind.id))));
            }
            p.getInventory().selected = 5;
        });
        shot(25, "27j_weapons");
        server(1, p -> GamePlayer.POWER.send(p, true));
        later(5, () -> setTab(2));
        shot(8, "27k_skills");
        // the frame rate with the window open and shut, each after a few seconds to settle (the counter updates once a second)
        later(70, () -> com.mojang.logging.LogUtils.getLogger().info("[FPS] status window open: {}", Minecraft.getInstance().getFps()));
        later(1, () -> Minecraft.getInstance().setScreen(null));
        later(70, () -> com.mojang.logging.LogUtils.getLogger().info("[FPS] no window: {}", Minecraft.getInstance().getFps()));
        // the op menu
        server(5, p -> com.example.charpowers.own.OpMenu.open(p));
        shot(12, "27o_op_general");
        later(70, () -> com.mojang.logging.LogUtils.getLogger().info("[FPS] op menu open: {}", Minecraft.getInstance().getFps()));
        later(1, () -> {
            try {
                var field = com.example.charpowers.client.OpScreen.class.getDeclaredField("picked");
                field.setAccessible(true);
                field.set(Minecraft.getInstance().screen, "green_lantern");
            } catch (ReflectiveOperationException | RuntimeException e) {
                CharPowers.LOG.error("[ClientCheck] op menu", e);
            }
        });
        shot(8, "27p_op_lantern");
        later(1, () -> Minecraft.getInstance().setScreen(null));
        // the power menu with its panels
        later(5, () -> {
            try {
                Minecraft.getInstance().setScreen(new net.threetag.palladium.client.screen.power.PowersScreen());
            } catch (Throwable t) {
                CharPowers.LOG.error("[ClientCheck] power menu", t);
            }
        });
        shot(25, "28_power_menu");
        later(1, () -> Minecraft.getInstance().setScreen(null));
        // Future Sight's glow
        server(5, p -> {
            FutureSight.POWER.give(p);
            Mastery.setLevel(p, FutureSight.POWER.id, 100);
            BlockPos at = p.blockPosition();
            for (int i = 3; i < 7; i++) p.serverLevel().setBlock(at.offset(-i, -2, i - 4), Blocks.DIAMOND_ORE.defaultBlockState(), 2);
            FutureSight.POWER.everySecond(p);
            p.connection.teleport(p.getX(), p.getY(), p.getZ(), 90f, 50f);
        });
        camera(1, CameraType.FIRST_PERSON);
        server(1, p -> {
            BlockPos at = p.blockPosition().below().east();
            p.serverLevel().setBlock(at, Blocks.STONE.defaultBlockState(), 2);
            p.connection.teleport(p.getX(), p.getY(), p.getZ(), 90f, 50f);
            p.gameMode.destroyBlock(at);
        });
        shot(45, "29_ore_glow");
        shot(10, "29b_ore_glow");
        shot(10, "29c_ore_glow");
        server(1, p -> p.connection.teleport(p.getX(), p.getY(), p.getZ(), 90f, 8f));
        later(5, () -> Minecraft.getInstance().setScreen(new com.example.charpowers.lantern.client.TunerScreen()));
        shot(25, "31_tuner");
        later(1, () -> Minecraft.getInstance().setScreen(null));
        later(5, () -> {
            CharPowers.LOG.info("[ClientCheck] finished");
            Minecraft.getInstance().stop();
        });
    }

    private static ItemStack stackOf(String id) {
        return new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(id)));
    }

    // python tools/run_real_client.py pack punchy=<jar>: with Punchy in the game, what it counts each weapon as, and a look at each held and mid-swing.
    // python tools/run_real_client.py pack pets: six pets round the player (a warden among them), one hurt, one fighting; panel and paws.
    // python tools/run_real_client.py features: the order wheel, a shadow soldier, the cat and the mastery toast.
    private static void featuresScript() {
        server(60, p -> {
            ServerLevel level = p.serverLevel();
            p.setGameMode(GameType.CREATIVE);
            p.getAbilities().flying = false;
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
            com.example.charpowers.mobcontrol.MobControl.POWER.give(p);
            p.connection.teleport(p.getX(), p.getY(), p.getZ(), 0f, 8f);
        });
        camera(5, CameraType.FIRST_PERSON);
        later(5, () -> com.example.charpowers.orders.client.OrderWheel.showForTest(2, 1, 1, 40, -34));
        shot(15, "f1_wheel_all");
        later(1, () -> com.example.charpowers.orders.client.OrderWheel.showForTest(0, 2, 0, -30, 5));
        shot(10, "f2_wheel_pets_only");
        later(1, com.example.charpowers.orders.client.OrderWheel::hideForTest);
        // shadow soldiers: a vanilla zombie and the custom gate creatures, tinted
        server(10, p -> {
            ServerLevel lv = p.serverLevel();
            java.util.List<Integer> ids = new java.util.ArrayList<>();
            int i = 0;
            java.util.List<net.minecraft.world.entity.EntityType<?>> types = new java.util.ArrayList<>();
            types.add(net.minecraft.world.entity.EntityType.ZOMBIE);
            for (com.example.charpowers.game.mob.GateKind k : com.example.charpowers.game.mob.GateKind.values()) if (!k.boss && types.size() < 4) types.add(com.example.charpowers.game.mob.GateMobs.mobType(k));
            for (var type : types) {
                var m = (net.minecraft.world.entity.Mob) type.create(lv);
                m.moveTo(p.getX() + (i - 1.5) * 2.2, p.getY(), p.getZ() + 6, 180f, 0f);
                m.setNoAi(true);
                m.setInvulnerable(true);
                m.setPersistenceRequired();
                lv.addFreshEntity(m);
                ids.add(m.getId());
                i++;
            }
            // one plain next to them for comparison
            var plain = net.minecraft.world.entity.EntityType.ZOMBIE.create(lv);
            plain.moveTo(p.getX() - 4.5, p.getY(), p.getZ() + 6, 180f, 0f);
            plain.setNoAi(true);
            plain.setInvulnerable(true);
            lv.addFreshEntity(plain);
            int[] list = ids.stream().mapToInt(Integer::intValue).toArray();
            Minecraft.getInstance().execute(() -> com.example.charpowers.game.client.ShadowClient.setForTest(list));
        });
        shot(40, "f3_shadows_front");
        server(2, p -> {
            p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, p.getBoundingBox().inflate(20)).forEach(net.minecraft.world.entity.Entity::discard);
            p.getServer().setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true);
            Minecraft.getInstance().execute(() -> com.example.charpowers.game.client.ShadowClient.setForTest());
        });
        // the cat: switched on, sits beside you once you have been still for a while
        server(5, p -> {
            com.example.charpowers.cat.CompanionCats.setOn(p, true);
            com.example.charpowers.cat.CompanionCats.look(p);
        });
        server(40, p -> {
            var cat = com.example.charpowers.cat.CompanionCats.catOf(p.getUUID());
            if (cat != null) cat.moveTo(p.getX(), p.getY(), p.getZ() + 2.0, 0f, 0f);
        });
        camera(2, CameraType.THIRD_PERSON_FRONT);
        shot(150, "f4_cat_sitting");
        server(2, p -> com.example.charpowers.cat.CompanionCats.cheer(p));
        shot(8, "f5_cat_hop");
        camera(2, CameraType.FIRST_PERSON);
        later(2, () -> Minecraft.getInstance().getToasts().addToast(new net.minecraft.client.gui.components.toasts.SystemToast(net.minecraft.client.gui.components.toasts.SystemToast.SystemToastIds.TUTORIAL_HINT, net.minecraft.network.chat.Component.literal("Advancement made"), net.minecraft.network.chat.Component.literal("a vanilla toast"))));
        later(2, () -> com.example.charpowers.mastery.client.MasteryToast.showForTest("charpowers:cloning", 24));
        shot(25, "f6_toast");
        later(1, () -> Minecraft.getInstance().stop());
    }

    private static void petsScript() {
        server(60, p -> {
            ServerLevel level = p.serverLevel();
            p.setGameMode(GameType.CREATIVE);
            p.getAbilities().flying = false;
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            p.connection.teleport(p.getX(), p.getY(), p.getZ(), 0f, 10f);
            level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true); // peaceful would remove the hostile pets
            var types = java.util.List.of(net.minecraft.world.entity.EntityType.WARDEN, net.minecraft.world.entity.EntityType.ZOMBIE, net.minecraft.world.entity.EntityType.SKELETON,
                    net.minecraft.world.entity.EntityType.CREEPER, net.minecraft.world.entity.EntityType.PIGLIN, net.minecraft.world.entity.EntityType.RAVAGER);
            int i = 0;
            for (var type : types) {
                var mob = (net.minecraft.world.entity.Mob) type.create(level);
                double x = p.getX() + (i - 2.5) * 2.2, z = p.getZ() + 6 + (i % 2) * 1.5;
                mob.moveTo(x, p.getY(), z, 180f, 0f);
                mob.setNoAi(true);
                mob.setInvulnerable(true);
                mob.setPersistenceRequired();
                level.addFreshEntity(mob);
                com.example.charpowers.own.Minions.claim(mob, p, "pet");
                com.example.charpowers.own.Minions.settle(mob);
                if (i == 1) mob.setHealth(mob.getMaxHealth() * 0.4f);
                if (i == 2) mob.setHealth(mob.getMaxHealth() * 0.15f);
                i++;
            }
            var cow = net.minecraft.world.entity.EntityType.COW.create(level);
            cow.moveTo(p.getX() + 12, p.getY(), p.getZ() + 12, 0f, 0f);
            cow.setNoAi(true);
            level.addFreshEntity(cow);
            com.example.charpowers.mobcontrol.PetMinds.order(p.getUUID(), cow);
        });
        shot(80, "pets_1_panel_and_paws");
        later(1, () -> com.example.charpowers.mobcontrol.client.PetHud.setMode(1));
        shot(10, "pets_2_paws_only");
        later(1, () -> com.example.charpowers.mobcontrol.client.PetHud.setMode(0));
        later(5, () -> {
            CharPowers.LOG.info("[ClientCheck] finished");
            Minecraft.getInstance().stop();
        });
    }

    private static void punchyScript() {
        server(60, p -> {
            ServerLevel level = p.serverLevel();
            p.setGameMode(GameType.CREATIVE);
            p.getAbilities().flying = false;
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            p.getInventory().clearContent();
            for (int i = 0; i < WEAPONS.length; i++) p.getInventory().setItem(i, stackOf(WEAPONS[i]));
            p.connection.teleport(p.getX(), p.getY(), p.getZ(), -90f, 0f);
        });
        later(40, ClientChecks::punchyKinds);
        for (int i = 0; i < WEAPONS.length; i++) {
            int slot = i;
            String name = "p" + i + "_" + WEAPONS[i].substring(WEAPONS[i].indexOf(':') + 1);
            later(5, () -> Minecraft.getInstance().player.getInventory().selected = slot);
            shot(40, name + "_a_held");
            later(1, () -> net.minecraft.client.KeyMapping.click(Minecraft.getInstance().options.keyAttack.getKey()));
            for (int frame = 10; frame < 24; frame++) shot(1, name + "_f" + frame);
            later(25, () -> {});
        }
        later(5, () -> {
            CharPowers.LOG.info("[ClientCheck] finished");
            Minecraft.getInstance().stop();
        });
    }

    // python tools/run_real_client.py pack lab punchy=<jar>: waits for run-real/lab/go ("slot camera frame,frame,..."), reloads the lab resource pack,
    // takes that weapon's picture held and at those ticks of a swing, then writes run-real/lab/done. run-real/lab/quit ends it.
    private static void labTick(Minecraft mc) {
        var folder = mc.gameDirectory.toPath().resolve("lab");
        try {
            if (java.nio.file.Files.exists(folder.resolve("quit"))) {
                java.nio.file.Files.delete(folder.resolve("quit"));
                mc.stop();
                return;
            }
            if (!STEPS.isEmpty() || !java.nio.file.Files.exists(folder.resolve("go"))) return;
            String[] order = java.nio.file.Files.readString(folder.resolve("go")).trim().split(" ");
            java.nio.file.Files.delete(folder.resolve("go"));
            int slot = Integer.parseInt(order[0]);
            later(1, () -> {});
            mc.reloadResourcePacks().thenRun(() -> {
                later(1, () -> mc.options.setCameraType(order[1].equals("back") ? CameraType.THIRD_PERSON_BACK : order[1].equals("front") ? CameraType.THIRD_PERSON_FRONT : CameraType.FIRST_PERSON));
                later(1, () -> mc.player.getInventory().selected = slot == 0 ? 1 : 0);
                later(3, () -> mc.player.getInventory().selected = slot);
                later(60, () -> {});
                later(1, () -> mc.player.getInventory().selected = slot == 0 ? 1 : 0);
                later(3, () -> mc.player.getInventory().selected = slot);
                shot(35, "lab_held");
                if (order.length > 2) {
                    later(1, () -> net.minecraft.client.KeyMapping.click(mc.options.keyAttack.getKey()));
                    int was = 0;
                    for (String frame : order[2].split(",")) {
                        shot(Integer.parseInt(frame) - was, "lab_f" + frame);
                        was = Integer.parseInt(frame);
                    }
                }
                later(10, () -> {
                    try {
                        java.nio.file.Files.writeString(folder.resolve("done"), "done");
                    } catch (java.io.IOException e) {
                        CharPowers.LOG.error("[Lab] done", e);
                    }
                });
            });
        } catch (java.io.IOException e) {
            CharPowers.LOG.error("[Lab]", e);
        }
    }

    // python tools/run_real_client.py pack steal: killed by a player, into the black room, aim at an icon, a real left click.
    // Vocal Expression words seen from the front, and the Ultimine outline.
    // ---- the menus at different window sizes and GUI scales

    private static void size(int ticks, int w, int h, int scale) {
        later(ticks, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.getWindow().setWindowed(w, h);
            mc.options.guiScale().set(scale);
            mc.resizeDisplay();
        });
    }

    // Puts the mouse somewhere without moving the real one (the first two doubles in MouseHandler are its x and y).
    private static void mouse(int ticks, double guiX, double guiY) {
        later(ticks, () -> {
            Minecraft mc = Minecraft.getInstance();
            double scale = mc.getWindow().getGuiScale();
            try {
                int n = 0;
                for (var f : net.minecraft.client.MouseHandler.class.getDeclaredFields()) {
                    if (f.getType() != double.class) continue;
                    f.setAccessible(true);
                    f.setDouble(mc.mouseHandler, (n == 0 ? guiX : guiY) * scale);
                    if (++n == 2) break;
                }
            } catch (ReflectiveOperationException e) {
                CharPowers.LOG.error("[ClientCheck] mouse", e);
            }
        });
    }

    private static void openPowers(int ticks) {
        later(ticks, () -> Minecraft.getInstance().setScreen(new net.threetag.palladium.client.screen.power.PowersScreen()));
    }

    private static void pickTab(int ticks, net.minecraft.resources.ResourceLocation power) {
        later(ticks, () -> {
            if (!(Minecraft.getInstance().screen instanceof net.threetag.palladium.client.screen.power.PowersScreen ps)) return;
            for (var tab : ((com.example.charpowers.mixin.PowersScreenAccessor) ps).charpowers$tabs()) {
                var holder = ((com.example.charpowers.mixin.PowerTabAccessor) tab).charpowers$holder();
                if (holder.getPower().getId().equals(power)) {
                    if (ps.selectedTab != null) ps.selectedTab.onClosed();
                    ps.selectedTab = tab;
                    tab.onOpened();
                }
            }
        });
    }

    private static void noToasts() {
        later(1, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            mc.getToasts().clear();
        });
    }

    private static void guiScript() {
        noToasts();
        server(200, p -> {
            ServerLevel level = p.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            p.setGameMode(GameType.CREATIVE);
            p.getAbilities().flying = false;
            p.connection.teleport(p.getX(), p.getY(), p.getZ(), -90f, 15f);
            net.threetag.palladium.power.SuperpowerUtil.addSuperpower(p, Mastery.CLONING);
            Mastery.setLevel(p, Mastery.CLONING, 45);
            GreenLantern.POWER.give(p);
            Mastery.setLevel(p, GreenLantern.POWER.id, 30);
            BookKeeping.POWER.give(p);
            Mastery.setLevel(p, BookKeeping.POWER.id, 1);
            InvincibleArmour.POWER.give(p);
            net.threetag.palladium.power.SuperpowerUtil.addSuperpower(p, com.example.charpowers.hidden.HiddenGrant.POWER);
            for (String id : com.example.charpowers.hidden.HiddenPowers.ids().stream().limit(24).toList()) com.example.charpowers.hidden.HiddenGrant.give(p, id);
        });
        noToasts();
        int[][] sizes = {{854, 480}, {1280, 720}, {1600, 900}, {1920, 1080}};
        int[] scales = {2, 3, 4, 0};
        // the power menu, every window size at every GUI scale
        for (int[] s : sizes) {
            for (int scale : scales) {
                size(4, s[0], s[1], scale);
                openPowers(12);
                pickTab(2, Mastery.CLONING);
                mouse(1, 1, 1);
                shot(22, "g1_menu_" + s[0] + "x" + s[1] + "_s" + scale);
                later(1, () -> Minecraft.getInstance().setScreen(null));
            }
        }
        // the narrowest a window can be before the GUI scale gives up: the side cards fold away
        size(4, 640, 480, 2);
        openPowers(12);
        pickTab(2, Mastery.CLONING);
        mouse(1, 1, 1);
        shot(22, "g1_menu_640x480_s2");
        mouse(1, 85, 99);
        shot(14, "g1_menu_640x480_s2_tooltip");
        later(1, () -> Minecraft.getInstance().setScreen(null));

        size(4, 1600, 900, 3);
        // hover, the other tabs, a list-style power, the buy box
        openPowers(12);
        pickTab(2, BookKeeping.POWER.id);
        mouse(20, 192, 129);
        shot(16, "g2_book_tab_hover");
        pickTab(2, com.example.charpowers.hidden.HiddenGrant.POWER);
        mouse(1, 1, 1);
        shot(14, "g2_hidden_tab");
        mouse(1, 200, 110);
        shot(14, "g2_hidden_tab_tooltip");
        mouse(1, 1, 1);
        pickTab(2, GreenLantern.POWER.id);
        shot(14, "g2_lantern_tab");
        later(1, () -> {
            var mc = Minecraft.getInstance();
            if (!(mc.screen instanceof net.threetag.palladium.client.screen.power.PowersScreen ps)) return;
            var holder = ((com.example.charpowers.mixin.PowerTabAccessor) ps.selectedTab).charpowers$holder();
            var list = net.threetag.palladium.client.screen.power.ListPowerTab.create(mc, ps, 0, holder);
            var tabs = ((com.example.charpowers.mixin.PowersScreenAccessor) ps).charpowers$tabs();
            tabs.set(tabs.indexOf(ps.selectedTab), list);
            ps.selectedTab = list;
            list.onOpened();
        });
        mouse(1, 200, 90);
        shot(14, "g2_list_tab");
        mouse(1, 1, 1);
        later(1, () -> {
            var mc = Minecraft.getInstance();
            if (!(mc.screen instanceof net.threetag.palladium.client.screen.power.PowersScreen ps)) return;
            var holder = ((com.example.charpowers.mixin.PowerTabAccessor) ps.selectedTab).charpowers$holder();
            var first = holder.getAbilities().values().iterator().next();
            ps.openOverlayScreen(new net.threetag.palladium.client.screen.power.BuyAbilityScreen(first.getReference(),
                    new net.threetag.palladium.power.ability.AbilityConfiguration.UnlockData(new net.threetag.palladium.util.icon.ItemIcon(net.minecraft.world.item.Items.DIAMOND), 3, net.minecraft.network.chat.Component.literal("Diamonds")), true, ps));
        });
        shot(30, "g2_buy_box");
        later(1, () -> Minecraft.getInstance().setScreen(null));
        pickFps("power menu open", "no menu");
        // survival: the normal inventory with Palladium's button
        server(5, p -> p.setGameMode(GameType.SURVIVAL));
        later(10, () -> Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(Minecraft.getInstance().player)));
        shot(12, "g3_inventory");
        later(1, () -> Minecraft.getInstance().setScreen(null));
        server(1, p -> p.setGameMode(GameType.CREATIVE));

        // every other menu, at a small and a big window, at GUI scales 2, 3 and 4, so small text can be checked
        int[][] checks = {{854, 480, 2}, {1600, 900, 2}, {1600, 900, 3}, {1920, 1080, 4}};
        server(4, p -> {
            BookKeeping.POWER.write(p, 0);
            BookKeeping.POWER.write(p, 1);
        });
        for (int[] c : checks) {
            String tag = "_" + c[0] + "_s" + c[2];
            size(4, c[0], c[1], c[2]);
            later(12, () -> {});
            server(1, p -> com.example.charpowers.own.OpMenu.open(p));
            shot(20, "g4_op_general" + tag);
            later(1, () -> {
                try {
                    var field = com.example.charpowers.client.OpScreen.class.getDeclaredField("picked");
                    field.setAccessible(true);
                    field.set(Minecraft.getInstance().screen, "green_lantern");
                } catch (ReflectiveOperationException | RuntimeException e) {
                    CharPowers.LOG.error("[ClientCheck] op menu", e);
                }
            });
            shot(10, "g4_op_power" + tag);
            later(1, () -> Minecraft.getInstance().setScreen(null));
            server(4, p -> BookKeeping.POWER.open(p));
            shot(20, "g4_book" + tag);
            later(1, () -> Minecraft.getInstance().setScreen(null));
            later(4, () -> Minecraft.getInstance().setScreen(new com.example.charpowers.cloning.client.CloneListScreen(null)));
            shot(25, "g4_clones" + tag);
            later(1, () -> com.example.charpowers.cloning.client.CloneClient.openMenu(0, "Clone 1", "mine", 18, 20, 16, "12 64 -30"));
            shot(20, "g4_clone_orders" + tag);
            later(1, () -> Minecraft.getInstance().setScreen(new com.example.charpowers.translation.client.VoicesScreen(null)));
            shot(20, "g4_voices" + tag);
            later(1, () -> Minecraft.getInstance().setScreen(new com.example.charpowers.lantern.client.TunerScreen()));
            shot(25, "g4_tuner" + tag);
            later(1, () -> Minecraft.getInstance().setScreen(null));
        }
        // the frame rate with each kind of menu open, at the size most people play (the counter updates once a second)
        size(4, 1600, 900, 3);
        server(14, p -> com.example.charpowers.own.OpMenu.open(p));
        later(70, () -> com.mojang.logging.LogUtils.getLogger().info("[FPS] op menu: {}", Minecraft.getInstance().getFps()));
        later(1, () -> Minecraft.getInstance().setScreen(new com.example.charpowers.cloning.client.CloneListScreen(null)));
        later(70, () -> com.mojang.logging.LogUtils.getLogger().info("[FPS] clones menu: {}", Minecraft.getInstance().getFps()));
        later(1, () -> Minecraft.getInstance().setScreen(new com.example.charpowers.lantern.client.TunerScreen()));
        later(70, () -> com.mojang.logging.LogUtils.getLogger().info("[FPS] tuner: {}", Minecraft.getInstance().getFps()));
        later(1, () -> Minecraft.getInstance().setScreen(null));
        // Game Player keeps its hologram: the inventory beside a notification, then its pages
        server(4, p -> {
            GamePlayer.POWER.give(p);
            p.setGameMode(GameType.SURVIVAL);
            p.getInventory().add(new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
        });
        for (int[] c : checks) {
            String tag = "_" + c[0] + "_s" + c[2];
            size(4, c[0], c[1], c[2]);
            server(14, p -> GamePlayer.POWER.send(p, true));
            shot(22, "g5_game_status" + tag);
            later(1, () -> setTab(2));
            shot(8, "g5_game_skills" + tag);
            later(1, () -> setTab(0));
            later(1, () -> Minecraft.getInstance().setScreen(null));
            server(2, p -> GamePlayer.POWER.note(p, "A screen opens only you can see. Press H."));
            later(4, () -> {
                Minecraft mc = Minecraft.getInstance();
                mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
            });
            shot(16, "g5_holo_inventory" + tag);
            later(1, () -> holoPage("EQUIPMENT"));
            shot(8, "g5_holo_equipment" + tag);
            later(1, () -> Minecraft.getInstance().setScreen(null));
        }
        size(4, 1600, 900, 3);
        server(14, p -> GamePlayer.POWER.send(p, true));
        later(70, () -> com.mojang.logging.LogUtils.getLogger().info("[FPS] status window: {}", Minecraft.getInstance().getFps()));
        later(1, () -> Minecraft.getInstance().setScreen(null));
        later(4, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        });
        later(70, () -> com.mojang.logging.LogUtils.getLogger().info("[FPS] hologram inventory: {}", Minecraft.getInstance().getFps()));
        later(1, () -> Minecraft.getInstance().setScreen(null));
        size(4, 1600, 900, 2);
        server(1, p -> {
            InvincibleArmour.POWER.give(p);
            p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(SuitItems.SUIT.get()));
        });
        shot(30, "g6_hud");
        later(30, () -> Minecraft.getInstance().stop());
    }

    // With other power mods in the game: every tab they add, at a roomy and a tight size, to see that nothing fights.
    private static void guiModsScript() {
        noToasts();
        server(300, p -> {
            ServerLevel level = p.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            p.setGameMode(GameType.SURVIVAL);
            net.threetag.palladium.power.SuperpowerUtil.addSuperpower(p, Mastery.CLONING);
            Mastery.setLevel(p, Mastery.CLONING, 45);
            for (String id : java.util.List.of("gabeupheroes:werewolf", "fsang:capes", "fsang_combat:combat", "fsang:angel", "eop:pyrokinesis")) {
                try {
                    net.threetag.palladium.power.SuperpowerUtil.addSuperpower(p, new net.minecraft.resources.ResourceLocation(id));
                } catch (Throwable t) {
                    CharPowers.LOG.info("[ClientCheck] no {}", id);
                }
            }
        });
        later(10, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        });
        shot(14, "m0_inventory");
        later(1, () -> Minecraft.getInstance().setScreen(null));
        int[][] sizes = {{1600, 900, 2}, {1600, 900, 3}, {1280, 720, 3}};
        for (int[] c : sizes) {
            String tag = "_" + c[0] + "_s" + c[2];
            size(4, c[0], c[1], c[2]);
            openPowers(14);
            for (int tab = 0; tab < 8; tab++) {
                int index = tab;
                later(2, () -> {
                    if (!(Minecraft.getInstance().screen instanceof net.threetag.palladium.client.screen.power.PowersScreen ps)) return;
                    var tabs = ((com.example.charpowers.mixin.PowersScreenAccessor) ps).charpowers$tabs();
                    if (index >= tabs.size()) return;
                    if (ps.selectedTab != null) ps.selectedTab.onClosed();
                    ps.selectedTab = tabs.get(index);
                    ps.selectedTab.onOpened();
                });
                shot(14, "m1_tab" + tab + tag);
            }
            later(1, () -> Minecraft.getInstance().setScreen(null));
        }
        later(20, () -> Minecraft.getInstance().stop());
    }

    private static void pickFps(String open, String shut) {
        openPowers(1);
        pickTab(2, Mastery.CLONING);
        later(70, () -> com.mojang.logging.LogUtils.getLogger().info("[FPS] {}: {}", open, Minecraft.getInstance().getFps()));
        later(1, () -> Minecraft.getInstance().setScreen(null));
        later(70, () -> com.mojang.logging.LogUtils.getLogger().info("[FPS] {}: {}", shut, Minecraft.getInstance().getFps()));
    }

    private static void smallScript() {
        server(220, p -> {
            ServerLevel level = p.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            p.setGameMode(GameType.CREATIVE);
            p.getAbilities().flying = false;
            com.example.charpowers.life.LifeEnjoyer.POWER.give(p);
            com.example.charpowers.mastery.Mastery.setAwakened(p, com.example.charpowers.mastery.Mastery.LIFE_ENJOYER);
            p.getInventory().setItem(0, new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE));
            p.getInventory().selected = 0;
            BlockPos at = p.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(at.offset(-5, 0, 5), at.offset(5, 8, 7))) level.setBlock(pos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
            p.connection.teleport(p.getX(), p.getY(), p.getZ(), 0f, 0f);
        });
        camera(5, CameraType.FIRST_PERSON);
        later(60, () -> {
            com.example.charpowers.life.ultimine.UltimineClient.setShape(2);
            com.example.charpowers.life.ultimine.UltimineClient.KEY.setDown(true);
        });
        shot(30, "u1_ultimine_outline");
        later(1, () -> com.example.charpowers.life.ultimine.UltimineClient.KEY.setDown(false));
        server(10, p -> {
            ServerLevel level = p.serverLevel();
            com.example.charpowers.vocal.VocalExpression.POWER.give(p);
            BlockPos at = p.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(at.offset(-5, 0, 5), at.offset(5, 8, 7))) level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            for (BlockPos pos : BlockPos.betweenClosed(at.offset(9, 0, -14), at.offset(10, 10, 14))) level.setBlock(pos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
            p.connection.teleport(p.getX(), p.getY(), p.getZ(), -90f, 0f);
        });
        server(20, p -> com.example.charpowers.vocal.Words.shout(p, "HELLO", 1, false));
        shot(25, "v1_word_front");
    }

    // python tools/run_real_client.py pack icons: the new pictures in every place they show.
    private static void iconScript() {
        server(220, p -> {
            ServerLevel level = p.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            p.setGameMode(GameType.CREATIVE);
            p.getAbilities().flying = false;
            for (String id : com.example.charpowers.hidden.HiddenPowers.ids()) com.example.charpowers.hidden.HiddenGrant.give(p, id);
            String[] keys = {"flick", "blink", "one_punch", "loud_meow", "meteor"};
            for (int i = 0; i < keys.length; i++) com.example.charpowers.hidden.HiddenRuntime.equip(p, true, i, keys[i]);
            String[] always = {"fire_fist", "cat_landing", "nine_lives", "untouchable", "storm_heart"};
            for (int i = 0; i < always.length; i++) com.example.charpowers.hidden.HiddenRuntime.equip(p, false, i, always[i]);
            com.example.charpowers.timecontrol.TimeControl.POWER.give(p);
            Mastery.setLevel(p, com.example.charpowers.timecontrol.TimeControl.POWER.id, 100);
            // the medallions of Death Steal, from the front, from the side and edge-on, in daylight
            ItemStack[] stacks = {com.example.charpowers.hidden.HiddenIcons.stack("fire_fist"), com.example.charpowers.hidden.HiddenIcons.stack("meteor"),
                    com.example.charpowers.icons.PowerIconItem.of(new net.minecraft.resources.ResourceLocation("charpowers", "textures/icons/time_control.png"))};
            float[] turns = {180f, 140f, 90f};
            for (int i = 0; i < stacks.length; i++) {
                var d = net.minecraft.world.entity.EntityType.ITEM_DISPLAY.create(level);
                d.setPos(p.getX() - 2.5 + i * 2.5, p.getY() + 1.6, p.getZ() + 4);
                var tag = d.saveWithoutId(new net.minecraft.nbt.CompoundTag());
                tag.put("item", stacks[i].save(new net.minecraft.nbt.CompoundTag()));
                tag.putString("item_display", "fixed");
                d.load(tag);
                d.setYRot(turns[i]);
                level.addFreshEntity(d);
            }
            p.connection.teleport(p.getX(), p.getY(), p.getZ(), 0f, 0f);
        });
        camera(5, CameraType.FIRST_PERSON);
        shot(70, "i1_world_and_key_bar");
        later(5, () -> {
            try {
                Minecraft.getInstance().setScreen(new net.threetag.palladium.client.screen.power.PowersScreen());
            } catch (Throwable t) {
                CharPowers.LOG.error("[ClientCheck] power menu", t);
            }
        });
        shot(30, "i2_power_menu_first_tab");
        later(1, () -> showPowerTab("hidden_powers"));
        shot(15, "i3_hidden_tab");
        later(1, () -> showPowerTab("time_control"));
        shot(15, "i4_time_control_tree");
        later(1, () -> Minecraft.getInstance().setScreen(null));
        // every made sound once, very quietly: a file the game can't read shows in the log
        later(5, () -> {
            int played = 0;
            for (var entry : net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getEntries()) {
                if (!entry.getKey().location().getNamespace().equals("charpowers")) continue;
                Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(entry.getValue(), 1f, 0.02f));
                played++;
            }
            CharPowers.LOG.info("[ClientCheck] played {} sounds", played);
        });
        later(60, () -> CharPowers.LOG.info("[ClientCheck] sounds done"));
        later(5, () -> {
            CharPowers.LOG.info("[ClientCheck] finished");
            Minecraft.getInstance().stop();
        });
    }

    // Opens the Palladium power menu tab of the power with this path.
    private static void showPowerTab(String power) {
        try {
            var screen = (net.threetag.palladium.client.screen.power.PowersScreen) Minecraft.getInstance().screen;
            var tabs = net.threetag.palladium.client.screen.power.PowersScreen.class.getDeclaredField("tabs");
            tabs.setAccessible(true);
            for (Object tab : (java.util.List<?>) tabs.get(screen)) {
                var one = (net.threetag.palladium.client.screen.power.PowerTab) tab;
                CharPowers.LOG.info("[ClientCheck] tab {}", one.getTitle().getString());
                if (one.getTitle().getString().toLowerCase().replace(' ', '_').equals(power)) screen.selectedTab = one;
            }
        } catch (Throwable t) {
            CharPowers.LOG.error("[ClientCheck] power tab", t);
        }
    }

    private static void stealScript() {
        server(220, p -> {
            ServerLevel level = p.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            p.setGameMode(GameType.SURVIVAL);
            com.example.charpowers.deathsteal.DeathSteal.POWER.give(p);
            var killer = new ServerPlayer(level.getServer(), level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "killer"));
            net.threetag.palladium.power.SuperpowerUtil.addSuperpower(killer, new net.minecraft.resources.ResourceLocation("charpowers", "the_miner"));
            CharPowers.LOG.info("[StealCheck] killer has {}", net.threetag.palladium.power.SuperpowerUtil.getSuperpowers(killer).size());
            CharPowers.LOG.info("[StealCheck] hurt {}", p.hurt(p.damageSources().playerAttack(killer), 9999f));
            CharPowers.LOG.info("[StealCheck] dead {}", p.isDeadOrDying());
        });
        later(40, () -> Minecraft.getInstance().player.respawn());
        shot(80, "s1_room");
        later(1, ClientChecks::stealLook);
        server(5, p -> {
            CharPowers.LOG.info("[StealCheck] server: in the pocket {} at {}", com.example.charpowers.own.Pocket.in(p), p.blockPosition());
            net.minecraft.world.entity.Interaction near = null;
            for (var e : p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Interaction.class, p.getBoundingBox().inflate(32))) {
                CharPowers.LOG.info("[StealCheck] server target {} box {}", e.blockPosition(), e.getBoundingBox());
                if (near == null || e.distanceToSqr(p) < near.distanceToSqr(p)) near = e;
            }
            if (near == null) return;
            double x = near.getX(), z = near.getZ() + 2;
            net.minecraft.world.phys.Vec3 eye = new net.minecraft.world.phys.Vec3(x, p.getY() + p.getEyeHeight(), z);
            net.minecraft.world.phys.Vec3 at = near.getBoundingBox().getCenter().subtract(eye);
            float pitch = (float) -Math.toDegrees(Math.atan2(at.y, Math.sqrt(at.x * at.x + at.z * at.z)));
            p.connection.teleport(x, p.getY(), z, 180f, pitch);
        });
        later(20, ClientChecks::stealLook);
        shot(1, "s2_aimed");
        later(1, () -> net.minecraft.client.KeyMapping.click(Minecraft.getInstance().options.keyAttack.getKey()));
        shot(20, "s3_punched");
        server(1, p -> CharPowers.LOG.info("[StealCheck] after the punch: in the pocket {}, has the power {}", com.example.charpowers.own.Pocket.in(p), net.threetag.palladium.power.SuperpowerUtil.hasSuperpower(p, new net.minecraft.resources.ResourceLocation("charpowers", "the_miner"))));
        later(20, () -> {
            CharPowers.LOG.info("[ClientCheck] finished");
            Minecraft.getInstance().stop();
        });
    }

    // python tools/run_real_client.py pack lanhost, then a second one with: pack guest. Like a friend joining your world: the guest is killed by the
    // host, wakes in the black room, and punches from its own game, over a real connection.
    private static void lanHostTick(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (guestSeen || server.getPlayerList().getPlayers().size() < 2) return;
        guestSeen = true;
        CharPowers.LOG.info("[LanCheck] the guest is in");
        server(220, host -> {
            ServerPlayer guest = host.getServer().getPlayerList().getPlayers().get(1);
            host.serverLevel().getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, host.getServer());
            guest.setGameMode(GameType.SURVIVAL);
            com.example.charpowers.deathsteal.DeathSteal.POWER.give(guest);
            // a killer with a lot to take, and a guest who can read the names: the fullest room there is
            for (String own : new String[] {"the_miner", "cloning", "green_lantern", "superior_authority", "time_control", "game_player", "book_keeping", "invincible_armour"}) {
                net.threetag.palladium.power.SuperpowerUtil.addSuperpower(host, new net.minecraft.resources.ResourceLocation("charpowers", own));
            }
            host.getServer().getCommands().performPrefixedCommand(host.createCommandSourceStack().withPermission(4), "hiddenpower giveall " + host.getGameProfile().getName());
            Mastery.setLevel(guest, com.example.charpowers.deathsteal.DeathSteal.POWER.id, 100);
            CharPowers.LOG.info("[LanCheck] killer has {} powers", net.threetag.palladium.power.SuperpowerUtil.getSuperpowers(host).size());
            CharPowers.LOG.info("[LanCheck] hurt {}", guest.hurt(guest.damageSources().playerAttack(host), 9999f));
        });
        server(140, host -> {
            ServerPlayer guest = host.getServer().getPlayerList().getPlayers().get(1);
            CharPowers.LOG.info("[LanCheck] guest in the pocket {} at {}", com.example.charpowers.own.Pocket.in(guest), guest.blockPosition());
            net.minecraft.world.entity.Interaction near = null;
            for (var e : guest.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Interaction.class, guest.getBoundingBox().inflate(32))) {
                CharPowers.LOG.info("[LanCheck] server target {} box {}", e.blockPosition(), e.getBoundingBox());
                if (near == null || e.distanceToSqr(guest) < near.distanceToSqr(guest)) near = e;
            }
            if (near == null) return;
            // 4 blocks off: past where a PC reports a hit, so only the room's own check can catch this punch
            double x = near.getX(), z = near.getZ() + 4;
            net.minecraft.world.phys.Vec3 eye = new net.minecraft.world.phys.Vec3(x, guest.getY() + guest.getEyeHeight(), z);
            net.minecraft.world.phys.Vec3 at = near.getBoundingBox().getCenter().subtract(eye);
            float pitch = (float) -Math.toDegrees(Math.atan2(at.y, Math.sqrt(at.x * at.x + at.z * at.z)));
            guest.connection.teleport(x, guest.getY(), z, 180f, pitch);
            CharPowers.LOG.info("[LanCheck] guest moved in front of it");
        });
        server(200, host -> {
            ServerPlayer guest = host.getServer().getPlayerList().getPlayers().get(1);
            CharPowers.LOG.info("[LanCheck] after the punch: guest in the pocket {}", com.example.charpowers.own.Pocket.in(guest));
        });
        later(60, () -> {
            CharPowers.LOG.info("[ClientCheck] finished");
            mc.stop();
        });
    }

    private static void lanGuestTick(Minecraft mc) {
        if (titleSeen && !created) {
            created = true;
            mc.options.pauseOnLostFocus = false;
            net.minecraft.client.gui.screens.ConnectScreen.startConnecting(mc.screen, mc, net.minecraft.client.multiplayer.resolver.ServerAddress.parseString("localhost:" + LAN_PORT), new net.minecraft.client.multiplayer.ServerData("lan", "localhost:" + LAN_PORT, false), false);
            return;
        }
        if (mc.player == null) return;
        if (mc.screen instanceof net.minecraft.client.gui.screens.DeathScreen) {
            mc.player.respawn();
            mc.setScreen(null);
        }
        if (guestTicks < 0 && mc.level.dimension() == com.example.charpowers.own.Pocket.LEVEL) {
            guestTicks = 0;
            CharPowers.LOG.info("[LanCheck] guest game: in the room");
        }
        if (guestTicks < 0) return;
        guestTicks++;
        if (guestTicks == 60) {
            Screenshot.grab(mc.gameDirectory, "g1_room.png", mc.getMainRenderTarget(), c -> {});
            stealLook();
        }
        if (guestTicks == 220) {
            stealLook();
            Screenshot.grab(mc.gameDirectory, "g2_aimed.png", mc.getMainRenderTarget(), c -> {});
            CharPowers.LOG.info("[LanCheck] guest game: punching, holding {}", mc.player.getMainHandItem());
            net.minecraft.client.KeyMapping.click(mc.options.keyAttack.getKey());
        }
        if (guestTicks == 250) {
            Screenshot.grab(mc.gameDirectory, "g3_punched.png", mc.getMainRenderTarget(), c -> {});
            CharPowers.LOG.info("[LanCheck] guest game: now in {}", mc.level.dimension().location());
        }
        if (guestTicks == 400) mc.stop();
    }

    // House Builder: the picker, the ghost, a house half way up, then every house from the front and from the side.
    private static void houseScript() {
        var builder = com.example.charpowers.house.HouseBuilder.POWER;
        server(40, p -> {
            ServerLevel level = p.serverLevel();
            p.setGameMode(GameType.CREATIVE);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            level.setDayTime(5000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            builder.give(p);
            Mastery.setLevel(p, builder.id, 60);
            try {
                java.nio.file.Path folder = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("charpowers/houses");
                java.nio.file.Files.createDirectories(folder);
                HouseSmokeTests.writeHut(folder.resolve("My_Own_Hut.schem"));
                com.example.charpowers.house.HouseCatalog.scan(p.getServer(), true, text -> CharPowers.LOG.info("[ClientCheck] {}", text));
            } catch (java.io.IOException e) {
                CharPowers.LOG.error("[ClientCheck] couldn't write the test file", e);
            }
        });
        server(100, builder::sendPicker);
        shot(30, "house_picker");
        later(2, () -> Minecraft.getInstance().setScreen(null));
        // the ghost, seen from a little way back and above: pink on open ground, then red with a wall in the way
        server(5, p -> p.connection.teleport(0.5, -50, -25.5, 0f, 20f));
        later(30, () -> com.example.charpowers.house.client.HouseClient.start("mountain_lodge", "Mountain Lodge", 30, 22, 30));
        later(5, () -> Minecraft.getInstance().options.hideGui = false);
        shot(30, "house_ghost");
        // (the player has dropped to the ground by now, so the plot is right in front of them)
        server(5, p -> {
            for (int y = -60; y < -54; y++) p.serverLevel().setBlock(new BlockPos(3, y, -15), Blocks.BRICKS.defaultBlockState(), 2);
        });
        shot(40, "house_ghost_blocked");
        server(1, p -> {
            for (int y = -60; y < -54; y++) p.serverLevel().setBlock(new BlockPos(3, y, -15), Blocks.AIR.defaultBlockState(), 2);
        });
        later(2, () -> {
            com.example.charpowers.house.client.HouseClient.stopForCheck();
            Minecraft.getInstance().options.hideGui = true;
        });
        server(2, p -> {
            Mastery.setLevel(p, builder.id, 100);
            builder.awaken(p);
        });
        server(10, builder::sendPicker);
        later(10, () -> Minecraft.getInstance().options.hideGui = false);
        shot(20, "house_picker_awakened");
        later(2, () -> {
            Minecraft.getInstance().setScreen(null);
            Minecraft.getInstance().options.hideGui = true;
        });
        // one built for real, tick by tick
        server(5, p -> {
            builder.resetCooldown(p);
            String no = builder.tryBuild(p, "oak_timber_house", 0, -61, 60, 2);
            if (no != null) CharPowers.LOG.error("[ClientCheck] house refused: {}", no);
            p.connection.teleport(0.5, -40, 90.5, 180f, 15f);
        });
        shot(170, "house_midway");
        server(2, p -> builder.finishJobs(p.getServer()));
        int i = 0;
        for (var entry : com.example.charpowers.house.model.HouseList.BUILT_IN) {
            int x = 200 + 100 * i++;
            String id = entry.id();
            server(10, p -> {
                builder.resetCooldown(p);
                p.connection.teleport(x + 0.5, -30, -70.5, 0f, 10f);
            });
            server(50, p -> {
                String no = builder.tryBuild(p, id, x, -61, 0, 0);
                if (no != null) CharPowers.LOG.error("[ClientCheck] {} refused: {}", id, no);
                builder.finishJobs(p.getServer());
                houseCamera(p, id, x, 25);
            });
            shot(110, id + "_front");
            server(1, p -> houseCamera(p, id, x, 125));
            shot(50, id + "_side");
            server(1, p -> houseCamera(p, id, x, 215));
            shot(50, id + "_back");
        }
        // the same house turned each of the other three ways, and a look inside
        for (int turn = 1; turn < 4; turn++) {
            int x = 200 + 100 * (10 + turn), t = turn;
            server(10, p -> {
                builder.resetCooldown(p);
                p.connection.teleport(x + 0.5, -30, -70.5, 0f, 10f);
            });
            server(50, p -> {
                String no = builder.tryBuild(p, "oak_timber_house", x, -61, 0, t);
                if (no != null) CharPowers.LOG.error("[ClientCheck] turned house refused: {}", no);
                builder.finishJobs(p.getServer());
                houseCamera(p, "oak_timber_house", x, 25);
            });
            shot(120, "turned_" + t);
        }
        // indoors at night: stand inside the cottage facing its door, the lanterns have to do the work
        server(5, p -> {
            p.serverLevel().setDayTime(18000);
            p.connection.teleport(199.5, -59, -2.5, 0f, 0f);
        });
        shot(120, "inside_cottage");
        later(20, () -> Minecraft.getInstance().stop());
    }

    private static void houseCamera(ServerPlayer p, String id, int x, double angle) {
        var plan = com.example.charpowers.house.HouseCatalog.get(p.getServer(), id);
        // eye level, far enough back that the whole plot is in view
        double dist = Math.max(Math.max(plan.w, plan.d) * 0.85, plan.h * 1.1) + 3;
        double ground = -61;
        double a = Math.toRadians(angle);
        double cx = x + 0.5 + Math.sin(a) * dist, cz = 0.5 + Math.cos(a) * dist, cy = ground + 4;
        double tx = x + 0.5, tz = 0.5, ty = ground + plan.h * 0.5;
        float yaw = (float) Math.toDegrees(Math.atan2(-(tx - cx), tz - cz));
        float pitch = (float) -Math.toDegrees(Math.atan2(ty - cy, Math.hypot(tx - cx, tz - cz)));
        p.connection.teleport(cx, cy, cz, yaw, pitch);
    }

    private static void stealLook() {
        Minecraft mc = Minecraft.getInstance();
        var hit = mc.hitResult;
        CharPowers.LOG.info("[StealCheck] client: looking at {} {}", hit == null ? null : hit.getType(), hit instanceof net.minecraft.world.phys.EntityHitResult e ? e.getEntity() : hit == null ? "" : hit.getLocation());
        for (var e : mc.level.entitiesForRendering()) {
            if (e instanceof net.minecraft.world.entity.Interaction box) CharPowers.LOG.info("[StealCheck] client target {} box {} pickable {}", box.blockPosition(), box.getBoundingBox(), box.isPickable());
        }
    }

    private static void punchyKinds() {
        try {
            Class<?> resolver = Class.forName("punchy.client.render.ToolKindResolver");
            Class<?> kind = Class.forName("punchy.client.renderer.layer.VanillaFirstPersonItemLayer$ToolKind");
            var resolve = resolver.getMethod("resolveItemKind", ItemStack.class, kind);
            for (String id : WEAPONS) CharPowers.LOG.info("[PunchyCheck] {} = {}", id, resolve.invoke(null, stackOf(id), null));
        } catch (ReflectiveOperationException e) {
            CharPowers.LOG.error("[PunchyCheck] couldn't ask Punchy", e);
        }
    }

    private static void pigInFront(ServerPlayer p) {
        Pig pig = new Pig(EntityType.PIG, p.level());
        pig.moveTo(p.getX() - 3.5, p.getY(), p.getZ(), 0f, 0f);
        pig.setNoAi(true);
        pig.setInvulnerable(true);
        p.level().addFreshEntity(pig);
        p.connection.teleport(p.getX(), p.getY(), p.getZ(), 90f, 6f);
    }

    private static void holoPage(String name) {
        try {
            var screen = Minecraft.getInstance().screen;
            var page = com.example.charpowers.game.client.HoloScreen.class.getDeclaredField("page");
            page.setAccessible(true);
            page.set(screen, Enum.valueOf((Class) page.getType(), name));
            var layout = com.example.charpowers.game.client.HoloScreen.class.getDeclaredMethod("layout");
            layout.setAccessible(true);
            layout.invoke(screen);
        } catch (ReflectiveOperationException | RuntimeException e) {
            CharPowers.LOG.error("[ClientCheck] holo page", e);
        }
    }

    private static void setTab(int tab) {
        try {
            var field = com.example.charpowers.game.client.GameScreen.class.getDeclaredField("tab");
            field.setAccessible(true);
            if (Minecraft.getInstance().screen != null) field.setInt(Minecraft.getInstance().screen, tab);
        } catch (ReflectiveOperationException e) {
            CharPowers.LOG.error("[ClientCheck] tab", e);
        }
    }

    @SubscribeEvent
    public static void onScreen(ScreenEvent.Init.Post e) {
        if (ON && (e.getScreen() instanceof TitleScreen || GUI_MODS && e.getScreen().getClass().getSimpleName().contains("KubeJSError"))) titleSeen = true;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (!ON || e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof net.threetag.palladium.client.screen.AddonPackLogScreen log) {
            CharPowers.LOG.warn("[ClientCheck] closing Palladium's Addon Pack Log screen: {} entries", log.logs.size());
            mc.setScreen(log.parent);
        }
        if (LAN_GUEST) {
            lanGuestTick(mc);
            return;
        }
        if (titleSeen && !created) {
            created = true;
            mc.options.pauseOnLostFocus = false;
            mc.options.renderDistance().set(6);
            mc.options.guiScale().set(2);
            mc.createWorldOpenFlows().createFreshLevel("smoke", new LevelSettings("smoke", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                    new WorldOptions(1L, Boolean.getBoolean("charpowers.itemcheck"), false), access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(Boolean.getBoolean("charpowers.itemcheck") ? WorldPresets.NORMAL : WorldPresets.FLAT).value().createWorldDimensions());
        }
        if (mc.player == null || mc.getSingleplayerServer() == null || mc.getSingleplayerServer().getPlayerList().getPlayers().isEmpty()) return;
        if (!scripted) {
            scripted = true;
            if (LAB) {
                server(60, p -> {
                    ServerLevel level = p.serverLevel();
                    p.setGameMode(GameType.CREATIVE);
                    p.getAbilities().flying = false;
                    level.setDayTime(6000);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
                    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
                    p.getInventory().clearContent();
                    for (int i = 0; i < WEAPONS.length; i++) p.getInventory().setItem(i, stackOf(WEAPONS[i]));
                    p.connection.teleport(p.getX(), p.getY(), p.getZ(), -90f, 0f);
                });
                later(5, () -> labReady = true);
            } else if (LAN_HOST) {
                var server = mc.getSingleplayerServer();
                server.setUsesAuthentication(false);
                server.publishServer(GameType.SURVIVAL, true, LAN_PORT);
                CharPowers.LOG.info("[LanCheck] open on {}", LAN_PORT);
            } else if (AnimChecks.ON) {
                CharPowers.LOG.info("[ClientCheck] AnimChecks has it from here");
            } else if (HOUSES) houseScript();
            else if (STEAL) stealScript();
            else if (SMALL) smallScript();
            else if (PETS) petsScript();
            else if (GUI_MODS) guiModsScript();
            else if (GUI) guiScript();
            else if (LANTERN_LAB) LanternLab.script();
            else if (STEAL) stealScript();
            else if (FEATURES) featuresScript();
            else if (LINEUP) lineupScript();
            else if (ICONS) iconScript();
            else if (Boolean.getBoolean("charpowers.itemcheck")) com.example.charpowers.smoke.ItemChecks.begin();
            else if (PUNCHY) punchyScript();
            else script();
            CharPowers.LOG.info("[ClientCheck] world is up, {} steps to go", STEPS.size());
        }
        if (LAB && labReady) labTick(mc);
        if (LAN_HOST) lanHostTick(mc);
        if (STEPS.isEmpty()) return;
        if (++wait >= STEPS.peek().after()) {
            wait = 0;
            try {
                STEPS.poll().what().run();
            } catch (Throwable t) {
                CharPowers.LOG.error("[ClientCheck] step failed", t);
            }
        }
    }
}
