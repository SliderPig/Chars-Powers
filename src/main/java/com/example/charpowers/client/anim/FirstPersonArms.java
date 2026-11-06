package com.example.charpowers.client.anim;

import com.example.charpowers.CharPowers;
import com.example.charpowers.client.anim.model.Body;
import com.example.charpowers.client.anim.model.Solver;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;

// In first person the game draws a stand-in arm that has nothing to do with your body. While a pose is playing we draw your two real
// arms instead, from your real shoulders, as part of the world, the same way other people see them. That's what lets your hands
// be on the minigun. The stand-in hands slide out of view as they come in.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class FirstPersonArms {
    // the same numbers the game uses to place a player model: its scale, and how far up its neck is
    private static final float MODEL_SCALE = 0.9375f, MODEL_UP = 1.501f;
    private static final double EYE_HEIGHT = 1.62;
    private static final float EYE_IN_MODEL = (float) (MODEL_UP - EYE_HEIGHT / MODEL_SCALE) * 16f;
    private static final Body BODY = new Body();
    private static final Solver.View VIEW = new Solver.View();

    private FirstPersonArms() {}

    private static boolean firstPerson(Minecraft mc) {
        return mc.player != null && mc.options.getCameraType().isFirstPerson() && mc.getCameraEntity() == mc.player;
    }

    @SubscribeEvent
    public static void onHand(RenderHandEvent e) {
        if (PlayerAnims.handsAway(e.getPartialTick()) >= 0.99f) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onStage(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || !PlayerAnims.anyActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (!firstPerson(mc)) return;
        LocalPlayer p = mc.player;
        PlayerAnims.State s = PlayerAnims.state(p.getId());
        if (s == null || p.isSpectator() || p.isInvisible() || mc.options.hideGui) return;
        boolean any = s.hold != null || (s.shot != null && s.shot.def.firstPerson);
        if (!any || !(mc.getEntityRenderDispatcher().getRenderer(p) instanceof PlayerRenderer renderer)) return;
        float partial = e.getPartialTick();

        BODY.rest();
        VIEW.netYaw = 0;
        // the arms turn with the camera as one, so they keep their place on the screen
        VIEW.pitch = 0;
        VIEW.armY = 2;
        VIEW.walk = 0;
        VIEW.firstPerson = true;
        PlayerAnims.solve(p, s, BODY, VIEW, partial, true);

        PoseStack pose = e.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        int light = mc.getEntityRenderDispatcher().getPackedLightCoords(p, partial);
        pose.pushPose();
        // from the camera to the player model's own space, as if the body faced where you look
        toModel(pose, 0, -EYE_HEIGHT, 0, p.getViewYRot(partial));
        toCamera(pose, p.getViewXRot(partial), s.hold != null ? PlayerAnims.table().gun.firstPerson : PlayerAnims.table().gun.firstPersonPoses);
        boolean items = s.hold == null && s.shot != null && s.shot.def.drawItems;
        if (BODY.didChange(Body.RIGHT_ARM)) drawArm(pose, buffers, light, p, renderer, HumanoidArm.RIGHT, BODY.part[Body.RIGHT_ARM], items);
        if (BODY.didChange(Body.LEFT_ARM)) drawArm(pose, buffers, light, p, renderer, HumanoidArm.LEFT, BODY.part[Body.LEFT_ARM], items);
        pose.popPose();
        buffers.endBatch();
    }

    // In first person, arms and gun stay where they are on the screen: turned with the look round the eye, and brought up into view.
    static void toCamera(PoseStack pose, float pitchDegrees, double[] shift) {
        pose.translate(0f, EYE_IN_MODEL / 16f, 0f);
        pose.mulPose(Axis.XP.rotationDegrees(pitchDegrees));
        pose.translate(0f, -EYE_IN_MODEL / 16f, 0f);
        pose.translate(shift[0] / 16, shift[1] / 16, shift[2] / 16);
    }

    // The same steps the game takes to put a player model in the world.
    static void toModel(PoseStack pose, double x, double y, double z, float bodyYaw) {
        pose.translate(x, y, z);
        pose.mulPose(Axis.YP.rotationDegrees(180f - bodyYaw));
        pose.scale(-1f, -1f, 1f);
        pose.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        pose.translate(0f, -MODEL_UP, 0f);
    }

    private static void drawArm(PoseStack pose, MultiBufferSource buffers, int light, LocalPlayer p, PlayerRenderer renderer, HumanoidArm side, double[] part, boolean items) {
        boolean right = side == HumanoidArm.RIGHT;
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        // The game's own arm drawing sets the arm up in its own way, then draws: everything else that dresses an arm (the Green Lantern suit,
        // Palladium's skins) hooks in there. So we let it draw, and first undo where it will put the arm.
        model.attackTime = 0f;
        model.crouching = false;
        model.swimAmount = 0f;
        model.setupAnim(p, 0f, 0f, 0f, 0f, 0f);
        ModelPart arm = right ? model.rightArm : model.leftArm;
        pose.pushPose();
        pose.translate(part[0] / 16f, part[1] / 16f, part[2] / 16f);
        pose.mulPose(new Quaternionf().rotationZYX((float) part[5], (float) part[4], (float) part[3]));
        pose.pushPose();
        pose.mulPose(new Quaternionf().rotationZYX(arm.zRot, arm.yRot, 0f).conjugate());
        pose.translate(-arm.x / 16f, -arm.y / 16f, -arm.z / 16f);
        if (right) renderer.renderRightHand(pose, buffers, light, p);
        else renderer.renderLeftHand(pose, buffers, light, p);
        pose.popPose();
        if (items) {
            ItemStack stack = right == (p.getMainArm() == HumanoidArm.RIGHT) ? p.getMainHandItem() : p.getOffhandItem();
            if (!stack.isEmpty()) {
                // what the game's held-item layer does
                pose.mulPose(Axis.XP.rotationDegrees(-90f));
                pose.mulPose(Axis.YP.rotationDegrees(180f));
                pose.translate((right ? 1 : -1) / 16f, 0.125f, -0.625f);
                Minecraft.getInstance().gameRenderer.itemInHandRenderer.renderItem(p, stack, right ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND, !right, pose, buffers, light);
            }
        }
        pose.popPose();
    }
}
