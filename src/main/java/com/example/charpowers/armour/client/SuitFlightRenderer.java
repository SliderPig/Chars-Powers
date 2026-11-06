package com.example.charpowers.armour.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.armour.SuitFlight;
import com.example.charpowers.armour.model.SuitMotion;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

// Draws the flying suit from the same model as the worn one, moved by SuitMotion's timeline: a closed bundle in the air, then standing behind
// you with its legs, back plate and arms swinging open, then shutting in turn, with the helmet hovering and dropping last.
public class SuitFlightRenderer extends EntityRenderer<SuitFlight> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(CharPowers.MODID, "textures/models/armor/invincible.png");
    private final HumanoidModel<LivingEntity> model = SuitModel.fresh();

    public SuitFlightRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0f;
    }

    @Override
    public void render(SuitFlight entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        double t = entity.timeline(partial);
        // from inside the suit the picture is the plates and visor drawn over the screen (SuitVisor), not the model round your eyes
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null && entity.who() == mc.player.getId() && mc.options.getCameraType().isFirstPerson() && SuitMotion.glide(t) > 0.4) return;
        float lean = (float) SuitMotion.lean(t);
        double legs = SuitMotion.open(0, t), chest = SuitMotion.open(1, t), arms = SuitMotion.open(2, t);
        model.young = false;
        model.setAllVisible(true);
        model.hat.visible = false;
        model.rightLeg.zRot = (float) (0.4 * legs);
        model.leftLeg.zRot = (float) (-0.4 * legs);
        model.rightArm.zRot = (float) (1.2 * arms);
        model.leftArm.zRot = (float) (-1.2 * arms);
        model.rightArm.xRot = model.leftArm.xRot = lean > 1 ? -0.2f : 0f;
        model.body.getChild("chest").xRot = (float) (-1.15 * chest);
        model.body.getChild("pack").xRot = (float) (0.95 * chest);
        model.head.y = (float) -SuitMotion.helmet(t);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180f - Mth.rotLerp(partial, entity.yRotO, entity.getYRot())));
        if (lean > 0.5f) {
            // flying with its head forward
            pose.translate(0, 0.9, 0);
            pose.mulPose(Axis.XP.rotationDegrees(-lean));
            pose.translate(0, -0.9, 0);
        }
        pose.scale(-1f, -1f, 1f);
        pose.translate(0, -1.501, 0);
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), t < SuitMotion.FLY ? 0xF000F0 : light, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(SuitFlight entity) {
        return TEXTURE;
    }
}
