package com.example.charpowers.items.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.items.CharItems;
import com.example.charpowers.items.FlameHammer;
import com.example.charpowers.items.GoldenLongSword;
import com.example.charpowers.items.IceStaff;
import com.example.charpowers.items.ItemNet;
import com.example.charpowers.items.AnimaKatana;
import com.example.charpowers.items.DaggerOfDarkness;
import com.example.charpowers.items.SprayBottle;
import com.example.charpowers.items.model.ComboRules;
import com.example.charpowers.items.model.WeaponStyle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Everything the weapons do on your PC: how they sit in the hands, your clicks, and when to lay down trail.
public final class ItemsClient {
    private static final ComboRules.State LOCAL = new ComboRules.State();
    private static final com.google.common.base.Supplier<Boolean> PUNCHY = com.google.common.base.Suppliers.memoize(() -> net.minecraftforge.fml.ModList.get().isLoaded("punchy"));

    private ItemsClient() {}

    // The arm poses. Only made when they're first needed.
    private static final class Poses {
        static final HumanoidModel.ArmPose TWO = HumanoidModel.ArmPose.create("CHARPOWERS_TWO_HANDED", true, Poses::apply);
        static final HumanoidModel.ArmPose ONE = HumanoidModel.ArmPose.create("CHARPOWERS_ONE_HANDED", false, Poses::apply);

        static void apply(HumanoidModel<?> model, LivingEntity e, HumanoidArm arm) {
            if (arm != e.getMainArm()) return;
            WeaponStyle style = styleOf(e.getMainHandItem().getItem());
            if (style != null) SwingTrack.poseModel(model, e, style, Minecraft.getInstance().getFrameTime());
        }
    }

    static WeaponStyle styleOf(Item item) {
        return com.example.charpowers.items.WeaponStyles.of(item);
    }

    public static IClientItemExtensions hands(WeaponStyle style) {
        return hands(() -> style);
    }

    // For items whose style can only be known once they're fully made (the System weapons: the game asks before their kind is set).
    public static IClientItemExtensions hands(java.util.function.Supplier<WeaponStyle> styleOf) {
        if (styleOf.get() == WeaponStyle.SPRAY) {
            return new IClientItemExtensions() {
                @Override
                public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                    return hand == InteractionHand.MAIN_HAND && entity.isUsingItem() ? Poses.ONE : null;
                }

                // holding the button points the bottle where you aim, and pumps it a little
                @Override
                public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack, float partial, float equip, float swing) {
                    if (!player.isUsingItem() || !(stack.getItem() instanceof SprayBottle)) return false;
                    float side = arm == HumanoidArm.RIGHT ? 1 : -1;
                    float pump = (float) Math.abs(Math.sin((player.tickCount + partial) * 1.05)) * 0.035f;
                    pose.translate(side * 0.42f, -0.42f + pump * 0.5f, -0.68f - pump);
                    pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(side * -10f));
                    return true;
                }
            };
        }
        return new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                if (hand != InteractionHand.MAIN_HAND) return null;
                WeaponStyle style = styleOf.get();
                if (style == null) return null;
                return style.twoHanded ? Poses.TWO : Poses.ONE;
            }

            @Override
            public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack, float partial, float equip, float swing) {
                WeaponStyle style = styleOf.get();
                if (style == null) return false;
                boolean main = arm == player.getMainArm();
                float[] f = main ? SwingTrack.handAt(player.getId(), style, SwingTrack.now(partial)) : SwingTrack.handAt(-1, style, 0);
                if (main && (!PUNCHY.get() || PunchyBridge.handedOver())) SwingTrack.drawArms(pose, player, arm, style, f, equip, partial);
                SwingTrack.applyHand(pose, arm, f, equip);
                return true;
            }
        };
    }

    // On this PC, this living thing swung one of these.
    public static void swung(LivingEntity e, WeaponStyle style) {
        SwingTrack.swung(e, style);
    }

    // The server says this entity is doing this move.
    public static void show(int id, WeaponStyle style, int step) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(id);
        if (!(e instanceof LivingEntity living)) return;
        if (e == mc.player) {
            // we guessed this already when you clicked: only correct it if the server disagrees
            SwingTrack.Track t = SwingTrack.of(id);
            if (t != null && t.style == style && t.step == step && SwingTrack.now(0) - t.start < 8) return;
            LOCAL.step = step;
            LOCAL.start = mc.level.getGameTime();
        }
        SwingTrack.start(living, style, step);
    }

    // The Long Sword's slam: a line of gold running along the ground.
    public static void groundLine(int kind, double x, double y, double z, float dirX, float dirZ, float length) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        List<double[]> left = new ArrayList<>(), right = new ArrayList<>();
        double side = 0.8;
        double gy = y;
        for (double along = 0; along <= length; along += 0.5) {
            double px = x + dirX * along, pz = z + dirZ * along;
            // follows the ground, up and down, like the shockwave itself
            BlockPos at = BlockPos.containing(px, gy, pz);
            for (int up = 2; up >= -3; up--) {
                BlockPos here = at.offset(0, up, 0);
                if (mc.level.getBlockState(here.below()).isFaceSturdy(mc.level, here.below(), net.minecraft.core.Direction.UP) && !mc.level.getBlockState(here).isFaceSturdy(mc.level, here, net.minecraft.core.Direction.UP)) {
                    gy = here.getY();
                    break;
                }
            }
            double wobble = Math.sin(along * 2.4) * 0.14;
            left.add(new double[]{px - dirZ * (side + wobble), gy + 0.07, pz + dirX * (side + wobble)});
            right.add(new double[]{px + dirZ * (side - wobble), gy + 0.07, pz - dirX * (side - wobble)});
        }
        Trails.groundLine(TrailStyle.GROUND_GOLD, left, right, SwingTrack.now(mc.getFrameTime()), 1f / 3f);
    }

    // ---- events on the game's main bus

    @Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
    public static final class Events {
        private static final DustParticleOptions GOLD_DUST = new DustParticleOptions(new Vector3f(1f, 0.78f, 0.2f), 0.7f);
        private static final DustParticleOptions SOUL_DUST = new DustParticleOptions(new Vector3f(0.72f, 0.36f, 1f), 0.6f);
        private static final DustParticleOptions DARK_DUST = new DustParticleOptions(new Vector3f(0.05f, 0.02f, 0.09f), 0.8f);
        private static final DustParticleOptions DARK_DUST_SMALL = new DustParticleOptions(new Vector3f(0.05f, 0.02f, 0.09f), 0.3f);
        private static final double[] BASE = new double[3], TIP = new double[3];

        private Events() {}

        @SubscribeEvent
        public static void onClick(InputEvent.InteractionKeyMappingTriggered e) {
            if (!e.isAttack()) return;
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer p = mc.player;
            if (p == null || p.isSpectator()) return;
            Item item = p.getMainHandItem().getItem();
            if (item instanceof GoldenLongSword) {
                // the whole click belongs to the combo: the server runs it, so it's fair whatever anyone's PC says
                e.setCanceled(true);
                e.setSwingHand(false);
                int target = mc.hitResult instanceof EntityHitResult hit ? hit.getEntity().getId() : -1;
                ItemNet.toServer(new ItemNet.SwordPress(target));
                if (ComboRules.press(LOCAL, mc.level.getGameTime()) == ComboRules.Outcome.STARTED) playLocal(p);
                return;
            }
            if (item instanceof FlameHammer) {
                hammerClick(e, mc, p, item);
                if (e.isCanceled()) return;
            }
            WeaponStyle style = styleOf(item);
            if (!com.example.charpowers.items.WeaponStyles.timedHits(style) || !(mc.hitResult instanceof EntityHitResult hit)) return;
            // our own swing, and the hit lands when the blade gets there: the server waits the same number of ticks
            e.setCanceled(true);
            e.setSwingHand(false);
            float n = SwingTrack.now(mc.getFrameTime());
            if (SwingTrack.busy(p.getId(), style, n, 0.6f)) return;
            SwingTrack.Track t = SwingTrack.of(p.getId());
            int prev = t != null && t.style == style && t.step != style.idle ? t.step : -1;
            int step = style.pick(prev, t == null ? 999 : n - t.start, p.isCrouching(), p.isSprinting());
            SwingTrack.start(p, style, step);
            p.swing(InteractionHand.MAIN_HAND);
            p.resetAttackStrengthTicker();
            ItemNet.toServer(new ItemNet.WeaponHit(hit.getEntity().getId(), step));
        }

        private static void playLocal(LocalPlayer p) {
            SwingTrack.start(p, WeaponStyle.GOLD, LOCAL.step);
            p.swing(InteractionHand.MAIN_HAND);
        }

        private static void hammerClick(InputEvent.InteractionKeyMappingTriggered e, Minecraft mc, LocalPlayer p, Item item) {
            if (p.getCooldowns().isOnCooldown(item) || mc.hitResult == null) return;
            float charge = p.getAttackStrengthScale(0.5f);
            Vec3 spot = null;
            if (mc.hitResult.getType() == HitResult.Type.BLOCK && charge >= (p.isShiftKeyDown() ? 0.7f : 0.9f)) {
                spot = mc.hitResult.getLocation();
            } else if (mc.hitResult.getType() == HitResult.Type.MISS && charge >= 0.9f) {
                // swung at the air: it comes down on whatever's under the hammer's head
                Vec3 ahead = p.position().add(p.getViewVector(1f).multiply(1, 0, 1).normalize().scale(2.5)).add(0, 1, 0);
                BlockHitResult down = p.level().clip(new ClipContext(ahead, ahead.add(0, -5, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
                if (down.getType() == HitResult.Type.BLOCK) spot = down.getLocation();
            }
            if (spot == null) return;
            e.setCanceled(true);
            e.setSwingHand(true);
            p.resetAttackStrengthTicker();
            ItemNet.toServer(new ItemNet.HammerSlam(spot.x, spot.y, spot.z));
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.isPaused()) return;
            SwingTrack.clientTick();
            PunchyBridge.tick();
            if (mc.level.getGameTime() % 10 == 0) {
                for (var pl : mc.level.players()) {
                    WeaponStyle s = styleOf(pl.getMainHandItem().getItem());
                    if (s != null && s.idle >= 0 && pl.distanceToSqr(mc.player) < 48 * 48) SwingTrack.idleFlourish(pl, s);
                }
            }
            if (mc.player != null && mc.player.getMainHandItem().getItem() instanceof GoldenLongSword && ComboRules.tick(LOCAL, mc.level.getGameTime()) >= 0) playLocal(mc.player);
        }

        // Lays down trail for everything mid-swing, then draws all the trails.
        @SubscribeEvent
        public static void onRender(RenderLevelStageEvent e) {
            if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            float partial = e.getPartialTick();
            float now = SwingTrack.now(partial);
            for (Map.Entry<Integer, SwingTrack.Track> en : SwingTrack.all().entrySet()) {
                if (SwingTrack.freeze >= 0) break; // a frozen picture of a move has no motion to leave a trail
                SwingTrack.Track t = en.getValue();
                float u = SwingTrack.recent(t, now);
                if (u < 0 || u > 1.02f) continue;
                TrailStyle style = trailOf(t.style);
                if (style == null) continue;
                SwingPoses.Clip clip = SwingPoses.clip(t.style, t.step);
                if (clip == null || u < clip.cutFrom() || u > clip.cutTo()) continue;
                Entity entity = mc.level.getEntity(en.getKey());
                if (!(entity instanceof AbstractClientPlayer p) || p.isRemoved()) continue;
                ItemStack held = p.getMainHandItem();
                if (styleOf(held.getItem()) != t.style) continue;
                // seen from your own eyes the hammer's arc is just a line straight up the middle of the screen
                if (t.style == WeaponStyle.HAMMER && p == mc.player && mc.options.getCameraType().isFirstPerson()) continue;
                float[] span = bladeSpan(t.style);
                if (span == null || !SwingTrack.blade(p, held, t.style, partial, span, BASE, TIP)) continue;
                Trails.sample(en.getKey(), style, BASE, TIP, now);
                if (now - t.lastSpark >= 1f) {
                    t.lastSpark = now;
                    sparks(mc, t.style);
                }
            }
            Trails.render(e.getPoseStack(), e.getCamera(), partial);
        }

        private static TrailStyle trailOf(WeaponStyle s) {
            return switch (s) {
                case GOLD -> TrailStyle.GOLD;
                case KATANA -> TrailStyle.SOUL;
                case DAGGER -> TrailStyle.DARK;
                case HAMMER -> TrailStyle.EMBER;
                case KNIGHT -> TrailStyle.KNIGHT;
                case CRIMSON -> TrailStyle.CRIMSON;
                case GRAVITY -> TrailStyle.GRAVITY;
                case SPEAR -> TrailStyle.SPEAR;
                case SCYTHE -> TrailStyle.SCYTHE;
                case COCYTUS -> TrailStyle.COCYTUS;
                default -> null;
            };
        }

        // Which part of the weapon's model leaves a trail: two spots in the model, in sixteenths. The 3D ones must match tools/make_weapon_models.py;
        // the flat pictures run corner to corner, the blade's far end at the top right.
        private static float[] bladeSpan(WeaponStyle s) {
            return switch (s) {
                case GOLD -> new float[]{8, 0, 8, 8, 32, 8};
                case KATANA -> new float[]{8, -4, 8, 6, 32, 8};
                case DAGGER -> new float[]{8, 8, 8, 8, 20, 8};
                case HAMMER -> new float[]{8, 16, 8, 8, 28, 8};
                case KNIGHT, CRIMSON, GRAVITY -> new float[]{6, 6, 8, 15.5f, 15.5f, 8};
                case SPEAR -> new float[]{12, 12, 8, 15.8f, 15.8f, 8};
                case SCYTHE -> new float[]{11, 15, 8, 2, 13, 8};
                case COCYTUS -> new float[]{11, 11, 8, 15.5f, 15.5f, 8};
                default -> null;
            };
        }

        private static void sparks(Minecraft mc, WeaponStyle s) {
            var r = mc.level.random;
            double x = TIP[0], y = TIP[1], z = TIP[2];
            double along = r.nextDouble();
            double mx = BASE[0] + (x - BASE[0]) * along, my = BASE[1] + (y - BASE[1]) * along, mz = BASE[2] + (z - BASE[2]) * along;
            switch (s) {
                case GOLD -> {
                    mc.level.addParticle(GOLD_DUST, true, mx, my, mz, 0, 0.01, 0);
                    if (r.nextBoolean()) mc.level.addParticle(ParticleTypes.WAX_ON, true, x, y, z, (r.nextDouble() - 0.5) * 2, r.nextDouble(), (r.nextDouble() - 0.5) * 2);
                    if (r.nextInt(3) == 0) mc.level.addParticle(ParticleTypes.END_ROD, true, mx, my, mz, (r.nextDouble() - 0.5) * 0.03, 0.01, (r.nextDouble() - 0.5) * 0.03);
                }
                case KATANA -> {
                    // right in front of your own eyes a big soul would just be a blur
                    if (!mc.options.getCameraType().isFirstPerson() && r.nextBoolean()) mc.level.addParticle(ParticleTypes.SOUL, true, mx, my, mz, (r.nextDouble() - 0.5) * 0.02, 0.03, (r.nextDouble() - 0.5) * 0.02);
                    mc.level.addParticle(SOUL_DUST, true, mx, my, mz, 0, 0.01, 0);
                    mc.level.addParticle(ParticleTypes.WITCH, true, x, y, z, 0, 0, 0);
                    mc.level.addParticle(ParticleTypes.REVERSE_PORTAL, true, mx, my, mz, 0, 0, 0);
                }
                case DAGGER -> {
                    boolean close = mc.options.getCameraType().isFirstPerson();
                    mc.level.addParticle(close ? DARK_DUST_SMALL : DARK_DUST, true, mx, my, mz, 0, 0.005, 0);
                    if (!close) {
                        mc.level.addParticle(DARK_DUST, true, x, y, z, 0, -0.01, 0);
                        mc.level.addParticle(ParticleTypes.SMOKE, true, mx, my, mz, 0, 0.005, 0);
                    }
                }
                case HAMMER -> mc.level.addParticle(ParticleTypes.FLAME, true, x, y, z, 0, 0.01, 0);
                default -> {}
            }
        }
    }

    // ---- start-up, on the mod's bus

    @Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        private Setup() {}

        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent e) {
            e.enqueueWork(() -> {
                Poses.TWO.isTwoHanded(); // new arm poses have to exist before anyone is first drawn
                ItemProperties.register(CharItems.SPRAY_BOTTLE.get(), new ResourceLocation(CharPowers.MODID, "liquid"), (stack, level, entity, seed) -> SprayBottle.liquid(stack).ordinal());
            });
        }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
            e.registerEntityRenderer(CharItems.ICE_SHARD.get(), ShardRenderer::new);
            e.registerEntityRenderer(CharItems.SOLGO.get(), SolgoRenderer::new);
            e.registerBlockEntityRenderer(CharItems.SWORD_STONE_ENTITY.get(), StoneSwordRenderer::new);
        }
    }
}
