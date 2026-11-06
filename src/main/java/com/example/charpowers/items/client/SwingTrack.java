package com.example.charpowers.items.client;

import com.example.charpowers.items.model.WeaponStyle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;

// Who is swinging what, on this PC, and from that: how their arms should sit, how the weapon moves in first person, and exactly where its blade is in the world
// so the trail can follow it. The trail works the blade out the same way the game draws it, so the two can't drift apart. Works for other players too.
public final class SwingTrack {
    private static final float RETURN_TICKS = 4f, BLEND_IN = 0.1f;
    private static final float ARM_LENGTH = 10f;
    private static final Map<Integer, Track> TRACKS = new HashMap<>();
    private static float ticks;
    // For pictures of a move: every swing shown frozen at this point (0 to 1). Negative = moving as normal.
    public static float freeze = -1;

    static final class Track {
        WeaponStyle style;
        int step;
        float start = -1000, length = 1;
        float[] fromBody, fromHand;
        float lastSpark = -1000;
    }

    private SwingTrack() {}

    static void clientTick() {
        ticks += 1f;
        if (TRACKS.size() > 32) TRACKS.values().removeIf(t -> ticks - t.start > 200);
    }

    public static float now(float partial) {
        return ticks + partial;
    }

    public static void start(LivingEntity e, WeaponStyle style, int step) {
        float partial = Minecraft.getInstance().getFrameTime();
        Track t = TRACKS.computeIfAbsent(e.getId(), k -> new Track());
        t.fromBody = bodyAt(e.getId(), style, now(partial));
        t.fromHand = handAt(e.getId(), style, now(partial));
        t.style = style;
        t.step = step;
        t.start = now(partial);
        t.length = style.length(step);
        Trails.cut(e.getId());
    }

    private static void swishSound(LivingEntity e, WeaponStyle style) {
        String sound = switch (style) {
            case KATANA -> "katana_swing";
            case DAGGER -> "dagger_swing";
            case HAMMER -> "gold_swing";
            default -> null;
        };
        if (sound != null) e.level().playLocalSound(e.getX(), e.getEyeY(), e.getZ(), com.example.charpowers.items.CharItems.sound(sound), net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, style == WeaponStyle.HAMMER ? 0.55f : 0.95f + e.getRandom().nextFloat() * 0.15f, false);
    }

    // A plain swing from a weapon that takes its turns itself: its moves alternate as long as you keep swinging.
    static void swung(LivingEntity e, WeaponStyle style) {
        Track t = TRACKS.get(e.getId());
        float n = now(Minecraft.getInstance().getFrameTime());
        if (t != null && t.style == style && n - t.start < t.length * 0.7f) return;
        int prev = t != null && t.style == style && t.step != style.idle ? t.step : -1;
        int next = style.pick(prev, t == null ? 999 : n - t.start, e.isCrouching(), e.isSprinting());
        start(e, style, next);
        swishSound(e, style);
    }

    static Track of(int id) {
        return TRACKS.get(id);
    }

    static Map<Integer, Track> all() {
        return TRACKS;
    }

    static float recent(Track t, float n) {
        return freeze >= 0 ? freeze : (n - t.start) / t.length;
    }

    // Is this one in the middle of a move?
    static boolean busy(int id, WeaponStyle style, float n, float upTo) {
        Track t = TRACKS.get(id);
        return t != null && t.style == style && t.step != style.idle && (n - t.start) / t.length < upTo;
    }

    // Now and then, someone standing about with a weapon that has one plays its idle flourish.
    static void idleFlourish(LivingEntity e, WeaponStyle style) {
        if (style.idle < 0) return;
        Track t = TRACKS.get(e.getId());
        float n = now(0);
        if (t != null && t.style == style && n - t.start < 160 + Math.floorMod(e.getId() * 37, 120)) return;
        if (t == null || t.style != style) {
            // just picked it up: wait a while before the first one
            Track fresh = TRACKS.computeIfAbsent(e.getId(), k -> new Track());
            fresh.style = style;
            fresh.step = style.idle;
            fresh.start = n;
            fresh.length = 1;
            return;
        }
        if (e.getDeltaMovement().horizontalDistanceSqr() > 0.003 || e.swinging) return;
        start(e, style, style.idle);
    }

    // ---- key tables, read at a point in time

    private static float ease(float u) {
        return u * u * (3 - 2 * u);
    }

    private static float[] bodyOf(SwingPoses.Body k) {
        return new float[]{k.ax(), k.ay(), k.az(), k.bx(), k.by(), k.bz(), k.twist(), k.lean()};
    }

    private static float[] handOf(SwingPoses.Hand k) {
        return new float[]{k.x(), k.y(), k.z(), k.yaw(), k.pitch(), k.roll(), k.spin()};
    }

    private static float[] mix(float[] a, float[] b, float u) {
        float[] o = new float[a.length];
        for (int i = 0; i < a.length; i++) o[i] = Mth.lerp(u, a[i], b[i]);
        return o;
    }

    private static float[] sampleBody(SwingPoses.Clip c, float u) {
        SwingPoses.Body[] keys = c.body();
        if (u <= keys[0].t()) return bodyOf(keys[0]);
        for (int i = 0; i < keys.length - 1; i++) {
            if (u <= keys[i + 1].t()) return mix(bodyOf(keys[i]), bodyOf(keys[i + 1]), ease((u - keys[i].t()) / (keys[i + 1].t() - keys[i].t())));
        }
        return bodyOf(keys[keys.length - 1]);
    }

    private static float[] sampleHand(SwingPoses.Clip c, float u) {
        SwingPoses.Hand[] keys = c.hand();
        if (u <= keys[0].t()) return handOf(keys[0]);
        for (int i = 0; i < keys.length - 1; i++) {
            if (u <= keys[i + 1].t()) return mix(handOf(keys[i]), handOf(keys[i + 1]), ease((u - keys[i].t()) / (keys[i + 1].t() - keys[i].t())));
        }
        return handOf(keys[keys.length - 1]);
    }

    static float[] bodyAt(int id, WeaponStyle style, float n) {
        float[] guard = bodyOf(SwingPoses.guardBody(style));
        Track t = TRACKS.get(id);
        if (t == null || t.style != style) return guard;
        SwingPoses.Clip clip = SwingPoses.clip(style, t.step);
        if (clip == null) return guard;
        float u = recent(t, n);
        if (u >= 1) {
            float back = freeze >= 0 ? 1 : (n - (t.start + t.length)) / RETURN_TICKS;
            return back >= 1 ? guard : mix(sampleBody(clip, 1), guard, ease(back));
        }
        float[] pose = sampleBody(clip, u);
        return u < BLEND_IN && t.fromBody != null && t.fromBody.length == pose.length ? mix(t.fromBody, pose, ease(u / BLEND_IN)) : pose;
    }

    static float[] handAt(int id, WeaponStyle style, float n) {
        float[] guard = handOf(SwingPoses.guardHand(style));
        Track t = TRACKS.get(id);
        if (t == null || t.style != style) return guard;
        SwingPoses.Clip clip = SwingPoses.clip(style, t.step);
        if (clip == null) return guard;
        float u = recent(t, n);
        if (u >= 1) {
            float back = freeze >= 0 ? 1 : (n - (t.start + t.length)) / RETURN_TICKS;
            return back >= 1 ? guard : mix(sampleHand(clip, 1), guard, ease(back));
        }
        float[] pose = sampleHand(clip, u);
        return u < BLEND_IN && t.fromHand != null && t.fromHand.length == pose.length ? mix(t.fromHand, pose, ease(u / BLEND_IN)) : pose;
    }

    // ---- arms

    private static final float TILT = (float) Math.toRadians(30);

    // The turn of an arm from which way it points and which way the blade should go. Returns {xRot, yRot, zRot} the way ModelPart uses them.
    private static float[] armAngles(float ax, float ay, float az, float bx, float by, float bz) {
        // the table's right/up/forward become the model's x/y/z, which point left, down and backward
        Vector3f d = new Vector3f(-ax, -ay, -az).normalize();
        Vector3f hint = new Vector3f(-bx, -by, -bz);
        Vector3f p = new Vector3f(hint).sub(new Vector3f(d).mul(hint.dot(d)));
        if (p.lengthSquared() < 1e-4f) p.set(0, 0, -1).sub(new Vector3f(d).mul(-d.z));
        p.normalize();
        // the columns of the arm's turn: its local y points along the arm and its local -z along the blade's side of it
        Vector3f colY = d, colZ = new Vector3f(p).negate(), colX = new Vector3f(colY).cross(colZ);
        float sy = -colX.z;
        float yRot = (float) Math.asin(Mth.clamp(sy, -1f, 1f));
        float cy = Mth.cos(yRot);
        float xRot, zRot;
        if (Math.abs(cy) > 1e-3f) {
            xRot = (float) Mth.atan2(colY.z, colZ.z);
            zRot = (float) Mth.atan2(colX.y, colX.x);
        } else {
            xRot = (float) Mth.atan2(-colZ.y, colY.y);
            zRot = 0;
        }
        return new float[]{xRot, yRot, zRot};
    }

    // Sets the player's arms (and a little of their body) for this weapon. Called by the game while it poses the model.
    static void poseModel(HumanoidModel<?> model, LivingEntity e, WeaponStyle style, float partial) {
        if (style == WeaponStyle.SPRAY) {
            // aims the bottle exactly where the player looks
            var arm = e.getMainArm() == HumanoidArm.LEFT ? model.leftArm : model.rightArm;
            arm.xRot = model.head.xRot - 1.5f;
            arm.yRot = model.head.yRot + (e.getMainArm() == HumanoidArm.LEFT ? 0.1f : -0.1f);
            arm.zRot = 0;
            return;
        }
        float n = now(partial);
        float[] f = bodyAt(e.getId(), style, n);
        boolean left = e.getMainArm() == HumanoidArm.LEFT;
        float[] arm = armAngles(left ? -f[0] : f[0], f[1], f[2], left ? -f[3] : f[3], f[4], f[5]);
        float twist = left ? -f[6] : f[6];
        // the game adds its own swing after this: not wanted, the table is the swing
        model.attackTime = 0;
        // a very slight bob so it doesn't look frozen when standing
        float bob = Mth.sin((e.tickCount + partial) * 0.08f) * 0.012f;
        model.body.yRot = twist;
        var main = left ? model.leftArm : model.rightArm;
        var other = left ? model.rightArm : model.leftArm;
        float side = left ? -1 : 1;
        // shoulders follow the twist and lean
        model.rightArm.x = -Mth.cos(twist) * 5f;
        model.rightArm.z = Mth.sin(twist) * 5f;
        model.leftArm.x = Mth.cos(twist) * 5f;
        model.leftArm.z = -Mth.sin(twist) * 5f;
        main.xRot = arm[0] + bob;
        main.yRot = arm[1];
        main.zRot = arm[2];
        if (style.twoHanded) {
            Matrix4f r = new Matrix4f().rotationZYX(main.zRot, main.yRot, main.xRot);
            // the grip is near the fist; the other hand goes just above it on the handle
            Vector3f grip = r.transformDirection(new Vector3f(-1f * side, ARM_LENGTH, -2f)).add(main.x, main.y, main.z);
            Vector3f bladeDir = r.transformDirection(new Vector3f(0, Mth.sin(TILT), -Mth.cos(TILT)));
            Vector3f target = new Vector3f(grip).add(new Vector3f(bladeDir).mul(3.5f + style.otherHand * 22f));
            Vector3f to = target.sub(other.x, other.y, other.z).normalize();
            float[] angles = armAnglesModel(to, bladeDir);
            other.xRot = angles[0];
            other.yRot = angles[1];
            other.zRot = angles[2];
        }
    }

    private static float[] armAnglesModel(Vector3f d, Vector3f hint) {
        Vector3f p = new Vector3f(hint).sub(new Vector3f(d).mul(hint.dot(d)));
        if (p.lengthSquared() < 1e-4f) p.set(0, 0, -1);
        p.normalize();
        Vector3f colY = new Vector3f(d), colZ = new Vector3f(p).negate(), colX = new Vector3f(colY).cross(colZ);
        float yRot = (float) Math.asin(Mth.clamp(-colX.z, -1f, 1f));
        float xRot = (float) Mth.atan2(colY.z, colZ.z);
        float zRot = (float) Mth.atan2(colX.y, colX.x);
        return new float[]{xRot, yRot, zRot};
    }

    // ---- first person

    static void applyHand(PoseStack pose, HumanoidArm arm, float[] f, float equip) {
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        pose.translate(side * f[0], f[1] - equip * 0.6f, f[2]);
        pose.mulPose(Axis.YP.rotationDegrees(side * f[3]));
        pose.mulPose(Axis.XP.rotationDegrees(f[4]));
        pose.mulPose(Axis.ZP.rotationDegrees(side * f[5]));
        pose.mulPose(Axis.YP.rotationDegrees(side * f[6]));
    }

    // Your own arms on a weapon in first person: from the bottom corners of the view to the grip, both hands on the two-handed ones. Mods that animate the hands themselves take over from this.
    static void drawArms(PoseStack pose, LocalPlayer player, HumanoidArm arm, WeaponStyle style, float[] f, float equip, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (player.isInvisible() || !(mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) return;
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        Vector3f grip = new Vector3f(side * f[0], f[1] - equip * 0.6f, f[2]);
        Vector3f axis = new Matrix4f().rotationY(side * f[3] * Mth.DEG_TO_RAD).rotateX(f[4] * Mth.DEG_TO_RAD).rotateZ(side * f[5] * Mth.DEG_TO_RAD).transformDirection(new Vector3f(0, 1, 0));
        int light = mc.getEntityRenderDispatcher().getPackedLightCoords(player, partial);
        var buffers = mc.renderBuffers().bufferSource();
        Vector3f mainHand = new Vector3f(grip).sub(new Vector3f(axis).mul(style.twoHanded ? 0.06f : 0f));
        oneArm(pose, renderer, player, arm == HumanoidArm.RIGHT, new Vector3f(side * 0.9f, -1.5f - equip * 0.6f, 0.02f), mainHand, buffers, light);
        if (style.twoHanded && player.getOffhandItem().isEmpty()) {
            Vector3f otherHand = new Vector3f(grip).add(new Vector3f(axis).mul(style.otherHand));
            oneArm(pose, renderer, player, arm != HumanoidArm.RIGHT, new Vector3f(-side * 0.9f, -1.5f - equip * 0.6f, 0.02f), otherHand, buffers, light);
        }
    }

    private static void oneArm(PoseStack pose, PlayerRenderer renderer, LocalPlayer player, boolean right, Vector3f from, Vector3f to, net.minecraft.client.renderer.MultiBufferSource buffers, int light) {
        var model = renderer.getModel();
        var armPart = right ? model.rightArm : model.leftArm;
        var sleeve = right ? model.rightSleeve : model.leftSleeve;
        Vector3f dir = new Vector3f(to).sub(from);
        float length = dir.length();
        if (length < 0.01f) return;
        dir.div(length);
        pose.pushPose();
        pose.translate(from.x, from.y, from.z);
        pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), dir));
        pose.scale(0.58f, length / 0.75f, 0.58f);
        pose.translate(right ? 6f / 16f : -6f / 16f, 0f, 0f);
        armPart.visible = true;
        sleeve.visible = true;
        armPart.x = right ? -5f : 5f;
        armPart.y = 2f;
        armPart.z = 0f;
        armPart.xRot = armPart.yRot = armPart.zRot = 0f;
        sleeve.copyFrom(armPart);
        var texture = player.getSkinTextureLocation();
        armPart.render(pose, buffers.getBuffer(net.minecraft.client.renderer.RenderType.entitySolid(texture)), light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        sleeve.render(pose, buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityTranslucent(texture)), light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    // ---- where the blade is

    // Fills base and tip with world spots for the given part of the weapon's model. pxBase and pxTip are heights in the model, in sixteenths. False if it can't be worked out now.
    static boolean blade(AbstractClientPlayer p, ItemStack stack, WeaponStyle style, float partial, float[] span, double[] base, double[] tip) {
        Minecraft mc = Minecraft.getInstance();
        float n = now(partial);
        BakedModel model = mc.getItemRenderer().getModel(stack, p.level(), p, p.getId());
        boolean firstPerson = p == mc.player && mc.options.getCameraType().isFirstPerson() && mc.getCameraEntity() == p;
        PoseStack ps = new PoseStack();
        if (firstPerson) {
            HumanoidArm arm = p.getMainArm();
            applyHand(ps, arm, handAt(p.getId(), style, n), 0);
            model.applyTransform(arm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, ps, arm == HumanoidArm.LEFT);
            ps.translate(-0.5, -0.5, -0.5);
            Matrix4f m = ps.last().pose();
            Vector4f a = m.transform(new Vector4f(span[0] / 16f, span[1] / 16f, span[2] / 16f, 1f)), b = m.transform(new Vector4f(span[3] / 16f, span[4] / 16f, span[5] / 16f, 1f));
            var cam = mc.gameRenderer.getMainCamera();
            // the hand is drawn with its own field of view: move the spot to where it looks like it is on screen
            float fov = mc.options.fov().get();
            float k = (float) (Math.tan(Math.toRadians(fov) / 2) / Math.tan(Math.toRadians(70.0) / 2));
            toWorld(cam, a, k, base);
            toWorld(cam, b, k, tip);
            return true;
        }
        float[] f = bodyAt(p.getId(), style, n);
        boolean left = p.getMainArm() == HumanoidArm.LEFT;
        float[] arm = armAngles(left ? -f[0] : f[0], f[1], f[2], left ? -f[3] : f[3], f[4], f[5]);
        float twist = left ? -f[6] : f[6];
        double x = Mth.lerp(partial, p.xo, p.getX()), y = Mth.lerp(partial, p.yo, p.getY()), z = Mth.lerp(partial, p.zo, p.getZ());
        float bodyYaw = Mth.rotLerp(partial, p.yBodyRotO, p.yBodyRot);
        ps.mulPose(Axis.YP.rotationDegrees(180f - bodyYaw));
        ps.scale(-1f, -1f, 1f);
        ps.scale(0.9375f, 0.9375f, 0.9375f);
        ps.translate(0, -1.501f, 0);
        boolean crouch = p.getPose() == Pose.CROUCHING;
        float armX = (left ? 1 : -1) * Mth.cos(twist) * 5f, armZ = (left ? -1 : 1) * Mth.sin(twist) * 5f, armY = 2f + (crouch ? 3.2f : 0);
        ps.translate(armX / 16f, armY / 16f, armZ / 16f);
        ps.last().pose().rotate(new Quaternionf().rotationZYX(arm[2], arm[1], arm[0]));
        ps.mulPose(Axis.XP.rotationDegrees(-90f));
        ps.mulPose(Axis.YP.rotationDegrees(180f));
        ps.translate((left ? -1 : 1) / 16f, 0.125f, -0.625f);
        model.applyTransform(left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ps, left);
        ps.translate(-0.5, -0.5, -0.5);
        Matrix4f m = ps.last().pose();
        Vector4f a = m.transform(new Vector4f(span[0] / 16f, span[1] / 16f, span[2] / 16f, 1f)), b = m.transform(new Vector4f(span[3] / 16f, span[4] / 16f, span[5] / 16f, 1f));
        double drop = crouch ? -0.125 : 0;
        base[0] = x + a.x;
        base[1] = y + a.y + drop;
        base[2] = z + a.z;
        tip[0] = x + b.x;
        tip[1] = y + b.y + drop;
        tip[2] = z + b.z;
        return true;
    }

    private static void toWorld(net.minecraft.client.Camera cam, Vector4f v, float k, double[] out) {
        Vec3 c = cam.getPosition();
        Vector3f left = cam.getLeftVector(), up = cam.getUpVector(), look = cam.getLookVector();
        float rx = v.x * k, ry = v.y * k, fz = -v.z;
        out[0] = c.x - left.x * rx + up.x * ry + look.x * fz;
        out[1] = c.y - left.y * rx + up.y * ry + look.y * fz;
        out[2] = c.z - left.z * rx + up.z * ry + look.z * fz;
    }
}
