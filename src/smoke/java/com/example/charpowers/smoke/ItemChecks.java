package com.example.charpowers.smoke;

import com.example.charpowers.CharPowers;
import com.example.charpowers.items.CharItems;
import com.example.charpowers.items.FlameBurst;
import com.example.charpowers.items.LongSwordMoves;
import com.example.charpowers.items.Solgo;
import com.example.charpowers.items.SprayBottle;
import com.example.charpowers.items.client.ItemsClient;
import com.example.charpowers.items.model.SolgoRules.Move;
import com.example.charpowers.items.model.SprayRules.Liquid;
import com.example.charpowers.items.model.WeaponStyle;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

// python tools/run_real_client.py items: a real game with the new weapons and Solgo, shots of each, then quits. Part of the test build only.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class ItemChecks {
    private static final String ONLY = System.getProperty("charpowers.itemonly", "");
    private static final Deque<Step> STEPS = new ArrayDeque<>();
    private static int wait;
    private static BlockPos arena;
    private static Solgo solgo;

    private record Step(int after, Runnable what) {}

    private ItemChecks() {}

    private static void later(int ticks, Runnable what) {
        STEPS.add(new Step(ticks, what));
    }

    private static void shot(int ticks, String name) {
        later(ticks, () -> {
            Screenshot.grab(Minecraft.getInstance().gameDirectory, name + ".png", Minecraft.getInstance().getMainRenderTarget(), c -> {});
        });
    }

    private static void server(int ticks, Consumer<ServerPlayer> what) {
        later(ticks, () -> {
            var server = Minecraft.getInstance().getSingleplayerServer();
            server.execute(() -> {
                try {
                    what.accept(server.getPlayerList().getPlayers().get(0));
                } catch (Throwable t) {
                    CharPowers.LOG.error("[ItemCheck] step failed", t);
                }
            });
        });
    }

    private static void camera(int ticks, CameraType type) {
        later(ticks, () -> Minecraft.getInstance().options.setCameraType(type));
    }

    private static void hold(int ticks, net.minecraft.world.item.Item item) {
        server(ticks, p -> {
            p.getInventory().selected = 0;
            p.getInventory().setItem(0, new ItemStack(item));
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
        });
    }

    private static void click(int ticks) {
        later(ticks, () -> KeyMapping.click(Minecraft.getInstance().options.keyAttack.getDefaultKey()));
    }

    private static void face(ServerPlayer p, float yaw, float pitch) {
        p.connection.teleport(p.getX(), p.getY(), p.getZ(), yaw, pitch);
    }

    private static Zombie dummy(ServerPlayer p, double dx, double dz) {
        ServerLevel level = p.serverLevel();
        Zombie z = EntityType.ZOMBIE.create(level);
        z.moveTo(p.getX() + dx, p.getY(), p.getZ() + dz, 0f, 0f);
        z.setNoAi(true);
        z.setPersistenceRequired();
        z.setInvulnerable(false);
        level.addFreshEntity(z);
        return z;
    }

    private static void clearMobs(ServerPlayer p) {
        p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, p.getBoundingBox().inflate(80), m -> true).forEach(net.minecraft.world.entity.Entity::discard);
        p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, p.getBoundingBox().inflate(80), m -> true).forEach(net.minecraft.world.entity.Entity::discard);
    }

    public static void begin() {
        later(60, () -> {});
        server(10, p -> {
            ServerLevel level = p.serverLevel();
            p.setGameMode(GameType.CREATIVE);
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
            // a clean stage up in the air, so every shot has the same background
            arena = p.blockPosition().atY(150);
            for (BlockPos b : BlockPos.betweenClosed(arena.offset(-30, -1, -30), arena.offset(30, -1, 30))) level.setBlock(b, Blocks.SMOOTH_STONE.defaultBlockState(), 2);
            for (BlockPos b : BlockPos.betweenClosed(arena.offset(-30, -2, -30), arena.offset(30, -2, 30))) level.setBlock(b, Blocks.STONE.defaultBlockState(), 2);
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            p.teleportTo(level, arena.getX() + 0.5, arena.getY(), arena.getZ() + 0.5, -90f, 0f);
        });
        later(60, () -> Minecraft.getInstance().options.hideGui = false);
        if (ONLY.contains("punchy")) punchyShots();
        if (ONLY.contains("swings")) swingFrames(false);
        if (ONLY.contains("fpframes")) swingFrames(true);
        if (ONLY.isEmpty() || ONLY.contains("weapons")) weapons();
        if (ONLY.isEmpty() || ONLY.contains("effects")) effects();
        if (ONLY.isEmpty() || ONLY.contains("solgo")) solgoShots();
        if (ONLY.isEmpty() || ONLY.contains("rock")) rockShots();
        later(40, () -> {
            CharPowers.LOG.info("[ItemCheck] all done");
            Minecraft.getInstance().stop();
        });
    }

    // ---- frames of every move: the swing is frozen at points along it, and each point is shot in first person and, with the hand hidden,
    // looking round at two stand-in players holding the same weapon, one side on and one facing you.
    private static final String[] SWING_ITEMS = {"golden_kings_long_sword", "anima_katana", "dagger_of_darkness", "flame_hammer", "ice_staff", "fork", "knight_killer", "crimson_edge",
            "gravity_blade", "grand_spear", "shadow_scythe", "cocytus_hammer"};
    private static final float[] FRAMES = {0.12f, 0.3f, 0.45f, 0.62f, 0.85f};
    private static final java.util.List<ServerPlayer> STANDINS = new java.util.ArrayList<>();

    private static ServerPlayer standIn(ServerPlayer p, String name, double dx, double dz, float yaw) {
        ServerLevel level = p.serverLevel();
        ServerPlayer d = new ServerPlayer(level.getServer(), level, new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(name.getBytes()), name));
        level.getServer().getPlayerList().placeNewPlayer(new dev.dubhe.curtain.features.player.patches.FakeClientConnection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), d);
        d.setGameMode(GameType.CREATIVE);
        d.teleportTo(level, p.getX() + dx, p.getY(), p.getZ() + dz, yaw, 0f);
        d.setYHeadRot(yaw);
        d.setYBodyRot(yaw);
        d.getPersistentData().putFloat("standYaw", yaw);
        STANDINS.add(d);
        return d;
    }

    // A shot taken once a few frames have really been drawn since things changed: on a slow PC frames lag behind ticks.
    private static String frameShot;
    private static int framesLeft;

    private static void frameShot(String name) {
        later(1, () -> {
            frameShot = name;
            framesLeft = 3;
        });
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.END || frameShot == null || --framesLeft > 0) return;
        String name = frameShot;
        frameShot = null;
        Screenshot.grab(Minecraft.getInstance().gameDirectory, name + ".png", Minecraft.getInstance().getMainRenderTarget(), c -> {});
    }

    // The stand-ins have no game of their own running them: they're ticked here, facing the way they were put.
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || STANDINS.isEmpty()) return;
        for (ServerPlayer d : STANDINS) {
            float yaw = d.getPersistentData().getFloat("standYaw");
            d.setYRot(yaw);
            d.setYHeadRot(yaw);
            d.yBodyRot = yaw;
            d.doTick();
        }
    }

    private static void lookClient(float yaw, float pitch, boolean hand) {
        Minecraft mc = Minecraft.getInstance();
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
        mc.player.yRotO = yaw;
        mc.player.xRotO = pitch;
        mc.player.setYHeadRot(yaw);
        mc.options.hideGui = !hand;
    }

    private static void swingFrames(boolean fpOnly) {
        camera(0, CameraType.FIRST_PERSON);
        server(2, p -> {
            clearMobs(p);
            face(p, -90f, 8f);
            if (!fpOnly) {
                standIn(p, "SideOn", -6, -2.4, 180f);
                standIn(p, "FacingYou", -6, 2.4, -90f);
            }
        });
        later(30, () -> {});
        for (String name : SWING_ITEMS) {
            var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation(CharPowers.MODID, name));
            var style = com.example.charpowers.items.WeaponStyles.of(item);
            if (style == null) continue;
            server(2, p -> {
                p.getInventory().selected = 0;
                p.getInventory().setItem(0, new ItemStack(item));
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
                for (ServerPlayer d : STANDINS) d.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            });
            later(14, () -> {});
            int moves = fpOnly ? 1 : style.moves();
            for (int m = 0; m < moves; m++) {
                int move = m;
                later(1, () -> {
                    com.example.charpowers.items.client.SwingTrack.freeze = -1;
                    Minecraft mc = Minecraft.getInstance();
                    for (var pl : mc.level.players()) com.example.charpowers.items.client.SwingTrack.start(pl, style, move);
                });
                for (int f = 0; f < FRAMES.length; f++) {
                    float u = FRAMES[f];
                    String tag = name + "_m" + move + "_f" + f;
                    later(1, () -> {
                        com.example.charpowers.items.client.SwingTrack.freeze = u;
                        lookClient(-90f, 8f, true);
                    });
                    frameShot("swing_" + tag + "_fp");
                    if (!fpOnly) {
                        later(1, () -> lookClient(90f, 6f, false));
                        frameShot("swing_" + tag + "_tp");
                    }
                }
            }
            later(1, () -> com.example.charpowers.items.client.SwingTrack.freeze = -1);
        }
        later(2, () -> lookClient(-90f, 8f, true));
        server(2, p -> {
            for (ServerPlayer d : STANDINS) d.getServer().getPlayerList().remove(d);
            STANDINS.clear();
        });
    }

    // With Punchy in the game: each weapon in first person, its guard and the middle of its swing.
    private static void punchyShots() {
        camera(0, CameraType.FIRST_PERSON);
        for (var item : new net.minecraft.world.item.Item[]{CharItems.LONG_SWORD.get(), CharItems.KATANA.get(), CharItems.FLAME_HAMMER.get(), CharItems.ICE_STAFF.get(), CharItems.DAGGER.get(), CharItems.FORK.get()}) {
            String name = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item).getPath();
            hold(6, item);
            shot(40, "punchy_" + name + "_guard");
            later(10, () -> Minecraft.getInstance().player.swing(InteractionHand.MAIN_HAND));
            shot(3, "punchy_" + name + "_a");
            shot(4, "punchy_" + name + "_b");
            shot(4, "punchy_" + name + "_c");
            shot(5, "punchy_" + name + "_d");
            later(30, () -> {});
        }
    }

    private static void weapons() {
        camera(0, CameraType.FIRST_PERSON);
        // the sword's combo, by real clicks, seen from your own eyes
        hold(10, CharItems.LONG_SWORD.get());
        server(2, p -> dummy(p, 3.5, 0));
        shot(30, "sword_fp_idle");
        click(10);
        shot(4, "sword_fp_slash1");
        click(9);
        shot(4, "sword_fp_slash2");
        click(9);
        shot(8, "sword_fp_slam");
        shot(4, "sword_fp_slam_line");
        click(5);
        shot(6, "sword_fp_thrust");
        server(2, p -> CharPowers.LOG.info("[ItemCheck] sword step after clicks: {}", LongSwordMoves.stepOf(p)));
        // and from behind and in front
        later(70, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK));
        shot(20, "sword_tp_idle");
        for (int i = 0; i < 4; i++) {
            int step = i;
            later(30, () -> ItemsClient.show(Minecraft.getInstance().player.getId(), WeaponStyle.GOLD, step));
            shot(step == 2 ? 8 : (step == 3 ? 6 : 4), "sword_tp_move" + step);
        }
        camera(40, CameraType.THIRD_PERSON_FRONT);
        for (int i = 0; i < 4; i++) {
            int step = i;
            later(30, () -> ItemsClient.show(Minecraft.getInstance().player.getId(), WeaponStyle.GOLD, step));
            shot(step == 2 ? 8 : (step == 3 ? 6 : 4), "sword_front_move" + step);
        }
        shot(30, "sword_front_idle");
        // katana
        camera(2, CameraType.FIRST_PERSON);
        hold(4, CharItems.KATANA.get());
        shot(30, "katana_fp_idle");
        for (int i = 0; i < 2; i++) {
            later(24, () -> Minecraft.getInstance().player.swing(InteractionHand.MAIN_HAND));
            shot(5, "katana_fp_cut" + i);
        }
        camera(30, CameraType.THIRD_PERSON_BACK);
        shot(10, "katana_tp_idle");
        for (int i = 0; i < 2; i++) {
            later(24, () -> Minecraft.getInstance().player.swing(InteractionHand.MAIN_HAND));
            shot(5, "katana_tp_cut" + i);
            shot(4, "katana_tp_after" + i);
        }
        // dagger
        camera(10, CameraType.FIRST_PERSON);
        hold(4, CharItems.DAGGER.get());
        shot(30, "dagger_fp_idle");
        later(16, () -> Minecraft.getInstance().player.swing(InteractionHand.MAIN_HAND));
        shot(3, "dagger_fp_cut");
        camera(16, CameraType.THIRD_PERSON_BACK);
        later(10, () -> Minecraft.getInstance().player.swing(InteractionHand.MAIN_HAND));
        shot(3, "dagger_tp_cut");
        // hammer
        camera(16, CameraType.FIRST_PERSON);
        hold(4, CharItems.FLAME_HAMMER.get());
        shot(30, "hammer_fp_idle");
        later(16, () -> Minecraft.getInstance().player.swing(InteractionHand.MAIN_HAND));
        shot(5, "hammer_fp_up");
        shot(6, "hammer_fp_down");
        camera(20, CameraType.THIRD_PERSON_BACK);
        shot(16, "hammer_tp_idle");
        later(10, () -> Minecraft.getInstance().player.swing(InteractionHand.MAIN_HAND));
        shot(5, "hammer_tp_up");
        shot(6, "hammer_tp_down");
        // staff and fork
        camera(20, CameraType.FIRST_PERSON);
        hold(4, CharItems.ICE_STAFF.get());
        shot(30, "staff_fp_idle");
        camera(4, CameraType.THIRD_PERSON_FRONT);
        shot(16, "staff_front_idle");
        camera(4, CameraType.FIRST_PERSON);
        hold(4, CharItems.FORK.get());
        shot(20, "fork_fp");
        camera(4, CameraType.THIRD_PERSON_FRONT);
        shot(16, "fork_front");
        camera(4, CameraType.FIRST_PERSON);
        server(2, ItemChecks::clearMobs);
    }

    private static void effects() {
        // flame ring, seen from behind
        camera(2, CameraType.THIRD_PERSON_BACK);
        hold(4, CharItems.FLAME_HAMMER.get());
        server(2, p -> {
            face(p, -90f, 18f);
            dummy(p, 4, 2);
            dummy(p, 7, -1);
        });
        later(16, () -> Minecraft.getInstance().player.swing(InteractionHand.MAIN_HAND));
        server(2, p -> FlameBurst.slam(p, p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(1.5)), 7f, true));
        shot(4, "flame_ring_early");
        shot(4, "flame_ring");
        shot(6, "flame_ring_late");
        later(50, () -> {});
        server(2, ItemChecks::clearMobs);
        // ice staff, a real right-click
        camera(2, CameraType.FIRST_PERSON);
        hold(4, CharItems.ICE_STAFF.get());
        server(2, p -> {
            face(p, -90f, 0f);
            frozenDummy = dummy(p, 9, 2.2);
            frozenDummy.setNoAi(false);
            frozenDummy.setPersistenceRequired();
            face(p, -76f, -2f);
        });
        later(20, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        });
        shot(3, "ice_flying");
        camera(1, CameraType.THIRD_PERSON_BACK);
        shot(3, "ice_flying_tp");
        shot(3, "ice_hit");
        shot(10, "ice_frozen");
        server(2, p -> CharPowers.LOG.info("[ItemCheck] after the ice shard: frost {}, stopped {}, slow {}", frozenDummy.getTicksFrozen(), frozenDummy.isNoAi(), frozenDummy.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)));
        camera(2, CameraType.FIRST_PERSON);
        server(30, ItemChecks::clearMobs);
        // spray bottle, one liquid at a time, held down for real
        for (Liquid l : new Liquid[]{Liquid.WATER, Liquid.LAVA, Liquid.MILK}) {
            server(2, p -> {
                ItemStack bottle = new ItemStack(CharItems.SPRAY_BOTTLE.get());
                SprayBottle.set(bottle, l, l.full);
                p.getInventory().selected = 0;
                p.getInventory().setItem(0, bottle);
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
                face(p, -90f, 5f);
                dummy(p, 4, 0).setSecondsOnFire(l == Liquid.WATER ? 20 : 0);
            });
            later(20, () -> Minecraft.getInstance().options.keyUse.setDown(true));
            shot(12, "spray_" + l.id());
            camera(1, CameraType.THIRD_PERSON_BACK);
            shot(6, "spray_" + l.id() + "_tp");
            camera(1, CameraType.FIRST_PERSON);
            later(1, () -> Minecraft.getInstance().options.keyUse.setDown(false));
            server(10, p -> {
                CharPowers.LOG.info("[ItemCheck] {} bottle has {} left, dummy health {}", l, SprayBottle.sprays(p.getMainHandItem()),
                        p.serverLevel().getEntitiesOfClass(Zombie.class, p.getBoundingBox().inflate(10)).stream().map(z -> z.getHealth()).toList());
                clearMobs(p);
            });
        }
    }

    private static void solgoShots() {
        // up close and from afar, a player for scale beside him
        hold(2, Items.AIR);
        server(2, p -> {
            clearMobs(p);
            ServerLevel level = p.serverLevel();
            solgo = Solgo.spawn(level, p.getX() + 16, p.getY(), p.getZ(), false);
            solgo.setYRot(90f);
            solgo.yBodyRot = 90f;
            solgo.setYHeadRot(90f);
            solgo.setNoAi(true);
            face(p, -90f, -14f);
        });
        shot(30, "solgo_day_16");
        server(2, p -> {
            p.teleportTo(p.getX() - 24, p.getY(), p.getZ());
            face(p, -90f, -4f);
        });
        shot(20, "solgo_day_40");
        server(2, p -> {
            p.teleportTo(p.getX() + 34, p.getY(), p.getZ());
            face(p, -90f, -38f);
        });
        shot(20, "solgo_day_6");
        // a look at him standing by a second body: an armour stand in a player's size, side on
        server(2, p -> {
            ServerLevel level = p.serverLevel();
            var stand = EntityType.ARMOR_STAND.create(level);
            stand.moveTo(solgo.getX() - 4, solgo.getY(), solgo.getZ() + 3, 0f, 0f);
            stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.PLAYER_HEAD));
            stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            level.addFreshEntity(stand);
            p.teleportTo(solgo.getX() - 4, solgo.getY(), solgo.getZ() + 18);
            face(p, 180f, -12f);
        });
        shot(20, "solgo_scale");
        // every move, side on
        Move[] moves = {Move.WALK, Move.RUN, Move.WINDUP, Move.SLASH, Move.CROUCH, Move.LUNGE, Move.SCREAM, Move.STARED};
        int[] waits = {26, 12, 20, 5, 20, 8, 22, 12};
        for (int i = 0; i < moves.length; i++) {
            Move m = moves[i];
            server(4, p -> solgo.begin(m));
            shot(waits[i], "solgo_move_" + m.name().toLowerCase());
        }
        server(2, p -> {
            p.serverLevel().setDayTime(18000);
            solgo.begin(Move.IDLE);
            p.teleportTo(solgo.getX() - 13, solgo.getY(), solgo.getZ() + 2);
            face(p, -80f, -20f);
        });
        shot(30, "solgo_night_13");
        server(2, p -> {
            p.teleportTo(solgo.getX() - 35, solgo.getY(), solgo.getZ());
            face(p, -90f, -5f);
        });
        shot(20, "solgo_night_35");
        // death
        server(2, p -> {
            p.serverLevel().setDayTime(6000);
            solgo.setNoAi(false);
            p.teleportTo(solgo.getX() - 14, solgo.getY(), solgo.getZ());
            face(p, -90f, -10f);
            solgo.hurt(p.damageSources().playerAttack(p), 100000f);
        });
        shot(30, "solgo_dying");
        shot(30, "solgo_dying_late");
        server(30, p -> {
            CharPowers.LOG.info("[ItemCheck] katana on the ground after Solgo: {}", !p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, p.getBoundingBox().inflate(40), i -> i.getItem().is(CharItems.KATANA.get())).isEmpty());
            clearMobs(p);
        });
        // a real fight: he walks at you, looked at and not. Watches the log for his moves
        server(2, p -> {
            p.setGameMode(GameType.SURVIVAL);
            p.getAbilities().invulnerable = true;
            p.onUpdateAbilities();
            solgo = Solgo.spawn(p.serverLevel(), p.getX() + 20, p.getY(), p.getZ(), false);
            face(p, -90f, -8f);
        });
        shot(50, "solgo_fight_1");
        shot(70, "solgo_fight_2");
        server(2, p -> {
            face(p, 90f, 0f);
            CharPowers.LOG.info("[ItemCheck] solgo move {} distance {}", solgo.move(), solgo.distanceTo(p));
        });
        shot(60, "solgo_fight_behind");
        server(2, p -> face(p, -90f, -8f));
        shot(6, "solgo_fight_turned");
        server(100, p -> {
            CharPowers.LOG.info("[ItemCheck] solgo move {} distance {}", solgo.move(), solgo.distanceTo(p));
            p.setGameMode(GameType.CREATIVE);
            clearMobs(p);
        });
    }

    private static BlockPos rockAt;
    private static Zombie frozenDummy;
    private static volatile boolean rockDone;

    // Holds the steps back until the server has finished something slow.
    private static void waitFor(java.util.function.BooleanSupplier ready, int maxTicks) {
        later(1, () -> {
            if (!ready.getAsBoolean() && maxTicks > 0) {
                java.util.ArrayDeque<Step> rest = new java.util.ArrayDeque<>(STEPS);
                STEPS.clear();
                waitFor(ready, maxTicks - 1);
                STEPS.addAll(rest);
            }
        });
    }
    private static net.minecraft.world.level.levelgen.structure.BoundingBox rockBox;

    private static void rockShots() {
        // /locate finds a rock in this normal world, then we go and look
        server(2, p -> {
            long began = System.currentTimeMillis();
            ServerLevel level = p.serverLevel();
            var reg = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
            Holder<Structure> rock = reg.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE, new ResourceLocation(CharPowers.MODID, "kings_rock")));
            var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(rock), p.blockPosition(), 100, false);
            CharPowers.LOG.info("[ItemCheck] locate took {} ms", System.currentTimeMillis() - began);
            rockDone = true;
            if (found == null) {
                CharPowers.LOG.error("[ItemCheck] no Kings Rock found");
                return;
            }
            var startHere = level.getChunk(found.getFirst()).getStartForStructure(rock.value());
            var box = startHere.getBoundingBox();
            rockAt = box.getCenter();
            rockBox = box;
            CharPowers.LOG.info("[ItemCheck] Kings Rock start chunk at {}, rock at {} ({} blocks away), box {}", found.getFirst(), rockAt, (int) Math.sqrt(rockAt.distSqr(p.blockPosition())), box);
            p.setGameMode(GameType.SPECTATOR);
            p.teleportTo(level, rockAt.getX() + 28.5, 90, rockAt.getZ() + 0.5, 90f, 25f);
        });
        waitFor(() -> rockDone, 900);
        later(200, () -> {});
        server(2, p -> {
            ServerLevel level = p.serverLevel();
            var start = level.structureManager().getStructureAt(rockAt, level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(new ResourceLocation(CharPowers.MODID, "kings_rock")));
            BlockPos sword = null;
            for (BlockPos b : BlockPos.betweenClosed(rockBox.minX(), rockBox.minY(), rockBox.minZ(), rockBox.maxX(), rockBox.maxY(), rockBox.maxZ())) {
                if (level.isLoaded(b) && level.getBlockState(b).is(CharItems.SWORD_STONE.get())) {
                    sword = b.immutable();
                    break;
                }
            }
            CharPowers.LOG.info("[ItemCheck] Kings Rock start valid {}, sword at {}, biome {}", start.isValid(), sword, sword == null ? "?" : level.getBiome(sword).unwrapKey());
            if (sword != null) {
                rockAt = sword;
                p.teleportTo(level, sword.getX() + 26.5, sword.getY() + 6, sword.getZ() + 0.5, 90f, 12f);
            }
        });
        shot(240, "rock_far");
        server(2, p -> p.teleportTo(p.serverLevel(), rockAt.getX() - 12.5, rockAt.getY() + 4, rockAt.getZ() - 12.5, -45f, 18f));
        shot(60, "rock_side");
        server(2, p -> p.teleportTo(p.serverLevel(), rockAt.getX() + 3.5, rockAt.getY() + 2.5, rockAt.getZ() + 3.5, 135f, 18f));
        shot(40, "rock_sword_close");
        server(2, p -> {
            p.setGameMode(GameType.CREATIVE);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            p.serverLevel().setDayTime(18000);
            p.teleportTo(p.serverLevel(), rockAt.getX() + 4.5, rockAt.getY() + 2.5, rockAt.getZ() + 4.5, 135f, 15f);
        });
        shot(50, "rock_sword_night");
        server(2, p -> {
            var state = p.serverLevel().getBlockState(rockAt);
            state.use(p.serverLevel(), p, InteractionHand.MAIN_HAND, new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(rockAt), net.minecraft.core.Direction.UP, rockAt, false));
            CharPowers.LOG.info("[ItemCheck] sword pulled: {} swords in inventory", p.getInventory().countItem(CharItems.LONG_SWORD.get()));
        });
        shot(6, "rock_sword_pulled");
        shot(40, "rock_slit");
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || STEPS.isEmpty() || frameShot != null) return;
        if (++wait >= STEPS.peek().after()) {
            wait = 0;
            try {
                STEPS.poll().what().run();
            } catch (Throwable t) {
                CharPowers.LOG.error("[ItemCheck] step failed", t);
            }
        }
    }
}
