package com.example.charpowers.client.anim;

import com.example.charpowers.client.anim.model.GunPose;
import com.example.charpowers.lantern.ConstructEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

// Draws the minigun where its holder's hands are, worked out fresh every frame from the holder, not from where the server last said
// the gun was. So it can't lag behind your hands, however fast you turn.
public final class HeldGun {
    private HeldGun() {}

    // Called with the pose stack at the entity's own spot. True if it's placed the gun, turned, and at the gun model's scale.
    public static boolean place(ConstructEntity gun, PoseStack pose, float partial) {
        AbstractClientPlayer p = PlayerAnims.holderOf(gun);
        if (p == null) return false;
        PlayerAnims.State s = PlayerAnims.state(p.getId());
        Minecraft mc = Minecraft.getInstance();
        double ax = Mth.lerp(partial, gun.xOld, gun.getX()), ay = Mth.lerp(partial, gun.yOld, gun.getY()), az = Mth.lerp(partial, gun.zOld, gun.getZ());
        boolean firstPerson = p == mc.player && mc.options.getCameraType().isFirstPerson() && mc.getCameraEntity() == p;
        float bodyYaw;
        double netYaw, pitch, armY;
        double fx, fy, fz;
        if (firstPerson) {
            Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
            bodyYaw = p.getViewYRot(partial);
            netYaw = 0;
            pitch = 0;
            armY = 2;
            fx = eye.x;
            fy = eye.y - 1.62;
            fz = eye.z;
        } else {
            bodyYaw = Mth.rotLerp(partial, p.yBodyRotO, p.yBodyRot);
            netYaw = Math.toRadians(Mth.wrapDegrees(Mth.rotLerp(partial, p.yHeadRotO, p.yHeadRot) - bodyYaw));
            pitch = Math.toRadians(Mth.lerp(partial, p.xRotO, p.getXRot()));
            armY = p.isCrouching() ? 5 : 2;
            Vec3 shift = mc.getEntityRenderDispatcher().getRenderer(p) instanceof PlayerRenderer r ? r.getRenderOffset(p, partial) : Vec3.ZERO;
            fx = Mth.lerp(partial, p.xOld, p.getX()) + shift.x;
            fy = Mth.lerp(partial, p.yOld, p.getY()) + shift.y;
            fz = Mth.lerp(partial, p.zOld, p.getZ()) + shift.z;
        }
        GunPose held = PlayerAnims.gunPose(p, s, armY, netYaw, pitch, partial);
        FirstPersonArms.toModel(pose, fx - ax, fy - ay, fz - az, bodyYaw);
        if (firstPerson) FirstPersonArms.toCamera(pose, p.getViewXRot(partial), PlayerAnims.table().gun.firstPerson);
        pose.translate(held.position[0] / 16, held.position[1] / 16, held.position[2] / 16);
        float[] q = held.turn.toQuaternion();
        pose.mulPose(new Quaternionf(q[0], q[1], q[2], q[3]));
        // the gun's own numbers have y up and z forward, the model's y down and z back
        pose.mulPose(Axis.XP.rotationDegrees(180f));
        float size = (float) held.size;
        pose.scale(size, size, size);
        return true;
    }
}
