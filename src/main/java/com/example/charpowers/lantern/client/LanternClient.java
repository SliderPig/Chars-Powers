package com.example.charpowers.lantern.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.Net;
import com.example.charpowers.client.OwnClient;
import com.example.charpowers.client.Soft;
import com.example.charpowers.lantern.BubbleEntity;
import com.example.charpowers.lantern.ConstructEntity;
import com.example.charpowers.lantern.LanternEntities;
import com.example.charpowers.lantern.SinestroMob;
import com.example.charpowers.mastery.Mastery;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.commands.Commands;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.threetag.palladium.power.ability.AbilityUtil;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

// Green Lantern on the PC: your battery on screen, the sword's clicks and the gun's trigger, your suit, and how constructs look.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class LanternClient {
    private static final ResourceLocation SUIT = new ResourceLocation(CharPowers.MODID, "textures/entity/lantern_suit.png");
    private static final ResourceLocation BUBBLE = new ResourceLocation(CharPowers.MODID, "textures/entity/bubble.png");
    private static final int PANEL = 0xFF0C1F13, TRACK = 0x60000000, GREEN = 0xFF31DA45, BRIGHT = 0xFF7DFF8E, TEXT = 0xFFEAFFEE, SOFT_TEXT = 0xFF9BD8A8;

    private static double battery = -1, max = 100;
    private static boolean sword, gun, firing;
    private static String oath = "";

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent e) {
        if (oath.isEmpty() || !e.getItemStack().is(com.example.charpowers.armour.SuitItems.POWER_BATTERY.get())) return;
        e.getToolTip().add(Component.literal("Your oath: " + oath).withStyle(ChatFormatting.GREEN));
        e.getToolTip().add(Component.literal("In your off hand, say it to recharge.").withStyle(ChatFormatting.GRAY));
        e.getToolTip().add(Component.literal("Crouch and use it to change your oath.").withStyle(ChatFormatting.GRAY));
    }

    static {
        OwnClient.on(Mastery.GREEN_LANTERN.toString(), LanternClient::receive);
    }

    private LanternClient() {}

    private static void receive(CompoundTag tag) {
        battery = tag.getDouble("battery");
        max = tag.getDouble("max");
        sword = tag.getBoolean("sword");
        gun = tag.getBoolean("gun");
        oath = tag.getString("oath");
    }

    private static void send(String action) {
        Net.CHANNEL.sendToServer(new Net.OwnToServer(Mastery.GREEN_LANTERN.toString(), action));
    }

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || com.example.charpowers.client.ui.HudStack.menuOpen() || battery < 0 || !AbilityUtil.hasPower(mc.player, Mastery.GREEN_LANTERN)) return;
        GuiGraphics g = e.getGuiGraphics();
        double part = battery / Math.max(1, max);
        float w = 92, h = 22, x = 6, y = com.example.charpowers.client.ui.HudStack.take(h);
        g.drawManaged(() -> {
            com.example.charpowers.client.ui.HudStack.chip(g, x, y, w, h, PANEL);
            com.example.charpowers.client.ui.Ui.text(g, "RING", x + 10, y + 4, SOFT_TEXT, com.example.charpowers.client.ui.Ui.XS);
            com.example.charpowers.client.ui.Ui.right(g, Math.round(part * 100) + "%", x + w - 10, y + 4, TEXT, com.example.charpowers.client.ui.Ui.XS);
            com.example.charpowers.client.ui.HudStack.bar(g, x + 10, y + 14, w - 20, 3, (float) part, battery >= max ? BRIGHT : GREEN);
        });
    }

    @SubscribeEvent
    public static void onCommands(RegisterClientCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("constructs").executes(c -> {
            Minecraft mc = Minecraft.getInstance();
            mc.tell(() -> mc.setScreen(new TunerScreen()));
            return 1;
        }));
    }

    // With the sword out a left click swings it. With the gun out the right button fires, and neither hits or uses what you're holding.
    @SubscribeEvent
    public static void onClick(InputEvent.InteractionKeyMappingTriggered e) {
        // looking at something that wants a click (Death Steal's icons), the click is for that
        boolean forSomethingElse = Minecraft.getInstance().hitResult instanceof net.minecraft.world.phys.EntityHitResult hit && hit.getEntity() instanceof net.minecraft.world.entity.Interaction;
        if (sword && e.isAttack() && !forSomethingElse) {
            send("swing");
            e.setCanceled(true);
            e.setSwingHand(false);
        } else if (gun && e.isUseItem()) {
            e.setCanceled(true);
            e.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (e.phase != TickEvent.Phase.END || mc.player == null) return;
        boolean want = gun && mc.options.keyUse.isDown() && mc.screen == null;
        if (want != firing) {
            firing = want;
            send(want ? "fire:1" : "fire:0");
        }
    }

    // The suit shows while you have the ring and haven't switched it off (the Suit Off toggle).
    public static boolean suitOn(net.minecraft.world.entity.player.Player p) {
        return AbilityUtil.hasPower(p, Mastery.GREEN_LANTERN) && !AbilityUtil.isEnabled(p, Mastery.GREEN_LANTERN, "suit_off");
    }

    // Your own arm in first person wears it too.
    @SubscribeEvent
    public static void onArm(RenderArmEvent e) {
        AbstractClientPlayer p = e.getPlayer();
        if (!suitOn(p) || !(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(p) instanceof PlayerRenderer renderer)) return;
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        model.attackTime = 0f;
        model.crouching = false;
        model.swimAmount = 0f;
        model.setupAnim(p, 0f, 0f, 0f, 0f, 0f);
        boolean right = e.getArm() == HumanoidArm.RIGHT;
        ModelPart arm = right ? model.rightArm : model.leftArm, sleeve = right ? model.rightSleeve : model.leftSleeve;
        arm.xRot = 0f;
        sleeve.xRot = 0f;
        VertexConsumer v = e.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(SUIT));
        arm.render(e.getPoseStack(), v, e.getPackedLight(), OverlayTexture.NO_OVERLAY);
        sleeve.render(e.getPoseStack(), v, e.getPackedLight(), OverlayTexture.NO_OVERLAY);
        e.setCanceled(true);
    }

    // ---- how things look

    public static final class ConstructModel extends GeoModel<ConstructEntity> {
        @Override
        public ResourceLocation getModelResource(ConstructEntity e) {
            return new ResourceLocation(CharPowers.MODID, "geo/construct/" + e.kind() + ".geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(ConstructEntity e) {
            return new ResourceLocation(CharPowers.MODID, "textures/entity/construct/" + e.kind() + (e.isYellow() ? "_yellow.png" : ".png"));
        }

        @Override
        public ResourceLocation getAnimationResource(ConstructEntity e) {
            return new ResourceLocation(CharPowers.MODID, "animations/construct/" + e.kind() + ".animation.json");
        }

        // The turret turns its head and tilts its gun at whatever it's shooting.
        @Override
        public void setCustomAnimations(ConstructEntity e, long instanceId, AnimationState<ConstructEntity> state) {
            if (!"turret".equals(e.kind()) || e.aimYaw() == 0f && e.aimPitch() == 0f) return;
            var head = getAnimationProcessor().getBone("Head");
            var gun = getAnimationProcessor().getBone("MiniGun");
            // the gun's barrel points out of the model's back, the side the renderer turns away from you
            if (head != null) head.setRotY((float) Math.toRadians(180f - e.aimYaw()));
            if (gun != null) gun.setRotX((float) Math.toRadians(e.aimPitch()));
        }
    }

    public static final class ConstructRenderer extends GeoEntityRenderer<ConstructEntity> {
        public ConstructRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new ConstructModel());
            shadowRadius = 0f;
        }

        @Override
        public RenderType getRenderType(ConstructEntity e, ResourceLocation texture, MultiBufferSource buffers, float partial) {
            return RenderType.entityTranslucentEmissive(texture);
        }

        // GeckoLib only turns living things to where they face, so constructs do it here. The swords and the gun tip up and down
        // with their aim too. The gun's barrel points out of the model's back, so it's turned round.
        @Override
        protected void applyRotations(ConstructEntity e, PoseStack pose, float age, float yaw, float partial) {
            // in someone's hands the gun goes exactly where they are this frame
            if (!com.example.charpowers.client.anim.HeldGun.place(e, pose, partial)) {
                super.applyRotations(e, pose, age, Mth.rotLerp(partial, e.yRotO, e.getYRot()), partial);
                float pitch = Mth.lerp(partial, e.xRotO, e.getXRot());
                if ("minigun".equals(e.kind())) {
                    pose.mulPose(Axis.YP.rotationDegrees(180f));
                    pose.mulPose(Axis.XP.rotationDegrees(pitch));
                    pose.scale(0.8f, 0.8f, 0.8f);
                } else if ("dagger".equals(e.kind())) {
                    pose.mulPose(Axis.XP.rotationDegrees(-pitch));
                }
            }
            // and whatever's been set in /constructs on this PC
            ConstructTweaks.Tweak t = ConstructTweaks.of(e.kind());
            pose.translate(t.x, t.y, t.z);
            pose.mulPose(Axis.YP.rotationDegrees((float) t.turn));
            pose.mulPose(Axis.XP.rotationDegrees((float) t.tilt));
            pose.mulPose(Axis.ZP.rotationDegrees((float) t.roll));
            pose.scale((float) t.size, (float) t.size, (float) t.size);
        }

        // Hard light glows in the dark.
        @Override
        public void render(ConstructEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            super.render(e, yaw, partial, pose, buffers, LightTexture.FULL_BRIGHT);
        }
    }

    // Sinestro is a person-shaped man, for now: my own model can replace this.
    public static final class SinestroRenderer extends HumanoidMobRenderer<SinestroMob, PlayerModel<SinestroMob>> {
        private static final ResourceLocation TEXTURE = new ResourceLocation(CharPowers.MODID, "textures/entity/sinestro.png");

        public SinestroRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        }

        @Override
        public ResourceLocation getTextureLocation(SinestroMob e) {
            return TEXTURE;
        }
    }

    // A green glass dome, drawn from strips.
    public static final class BubbleRenderer extends EntityRenderer<BubbleEntity> {
        private static final int AROUND = 24, UP = 8;

        public BubbleRenderer(EntityRendererProvider.Context ctx) {
            super(ctx);
        }

        @Override
        public ResourceLocation getTextureLocation(BubbleEntity e) {
            return BUBBLE;
        }

        @Override
        public void render(BubbleEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            float r = e.radius();
            VertexConsumer v = buffers.getBuffer(RenderType.entityTranslucent(BUBBLE));
            Matrix4f m = pose.last().pose();
            Matrix3f n = pose.last().normal();
            for (int up = 0; up < UP; up++) {
                double a0 = Math.PI / 2 * up / UP, a1 = Math.PI / 2 * (up + 1) / UP;
                for (int round = 0; round < AROUND; round++) {
                    double b0 = Math.PI * 2 * round / AROUND, b1 = Math.PI * 2 * (round + 1) / AROUND;
                    corner(v, m, n, r, a0, b0, 0, 0);
                    corner(v, m, n, r, a0, b1, 1, 0);
                    corner(v, m, n, r, a1, b1, 1, 1);
                    corner(v, m, n, r, a1, b0, 0, 1);
                }
            }
            super.render(e, yaw, partial, pose, buffers, light);
        }

        private static void corner(VertexConsumer v, Matrix4f m, Matrix3f n, float r, double up, double round, float tu, float tv) {
            float x = (float) (Math.cos(up) * Math.cos(round)), y = (float) Math.sin(up), z = (float) (Math.cos(up) * Math.sin(round));
            v.vertex(m, x * r, y * r, z * r).color(255, 255, 255, 110).uv(tu * 2, tv * 2).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_BRIGHT).normal(n, x, y, z).endVertex();
        }
    }

    // Your suit: drawn over your skin and its second layer while you have the ring. The skin itself is never touched.
    private static final class SuitLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        SuitLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p, float limb, float amount, float partial, float age, float head, float pitch) {
            if (p.isInvisible() || !suitOn(p)) return;
            renderColoredCutoutModel(getParentModel(), SUIT, pose, buffers, light, p, 1f, 1f, 1f);
        }
    }

    @Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        private Setup() {}

        @SubscribeEvent
        public static void setup(FMLClientSetupEvent e) {
            ConstructTweaks.load();
            ConstructEntity.speed = ConstructTweaks::speed;
            ConstructEntity.whileHeld = com.example.charpowers.client.anim.PlayerAnims::gunTick;
        }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
            e.registerEntityRenderer(LanternEntities.CONSTRUCT.get(), ConstructRenderer::new);
            e.registerEntityRenderer(LanternEntities.BUBBLE.get(), BubbleRenderer::new);
            e.registerEntityRenderer(LanternEntities.SINESTRO.get(), SinestroRenderer::new);
        }

        @SubscribeEvent
        @SuppressWarnings({"unchecked", "rawtypes"})
        public static void layers(EntityRenderersEvent.AddLayers e) {
            for (String skin : e.getSkins()) {
                LivingEntityRenderer renderer = e.getSkin(skin);
                if (renderer != null) renderer.addLayer(new SuitLayer(renderer));
            }
        }
    }
}
