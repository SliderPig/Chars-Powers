package com.example.charpowers.client.anim;

import com.example.charpowers.CharPowers;
import com.example.charpowers.client.anim.model.Body;
import com.example.charpowers.client.anim.model.Solver;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.threetag.palladium.client.model.animation.PalladiumAnimation;
import net.threetag.palladium.event.PalladiumClientEvents;

// The way into Palladium's own animation system: it asks us about every player it draws, in every view but first person, right after the
// game has worked out their walking and looking. Palladium runs animations low priority first, so its flight (-10) has gone
// before us and its aiming (100) comes after.
public final class PoseAnimation extends PalladiumAnimation {
    private final Body body = new Body();
    private final Solver.View view = new Solver.View();

    private PoseAnimation() {
        super(50);
    }

    // From the mod's constructor: Palladium collects these on the first resource load.
    public static void register() {
        PalladiumClientEvents.REGISTER_ANIMATIONS.register(add -> add.accept(new ResourceLocation(CharPowers.MODID, "poses"), new PoseAnimation()));
    }

    @Override
    public void animate(Builder b, AbstractClientPlayer p, HumanoidModel<?> model, FirstPersonContext context, float partial) {
        if (context.firstPerson() || !PlayerAnims.anyActive()) return;
        PlayerAnims.State s = PlayerAnims.state(p.getId());
        if (s == null || s.empty()) return;
        double free = PlayerAnims.bodyFree(p, partial);
        if (free <= 0) return;

        read(model.head, Body.HEAD);
        read(model.body, Body.CHEST);
        read(model.rightArm, Body.RIGHT_ARM);
        read(model.leftArm, Body.LEFT_ARM);
        read(model.rightLeg, Body.RIGHT_LEG);
        read(model.leftLeg, Body.LEFT_LEG);
        body.changed = 0;
        body.rootChanged = false;
        for (int i = 0; i < 3; i++) body.lift[i] = body.tip[i] = 0;
        view.netYaw = Math.toRadians(b.getNetHeadYaw());
        view.pitch = Math.toRadians(b.getHeadPitch());
        view.armY = model.crouching ? 5 : 2;
        view.walk = Math.min(1, p.walkAnimation.speed(partial) * 2.5);
        PlayerAnims.solve(p, s, body, view, partial, false);

        write(b, PlayerModelPart.HEAD, Body.HEAD, free);
        write(b, PlayerModelPart.CHEST, Body.CHEST, free);
        write(b, PlayerModelPart.RIGHT_ARM, Body.RIGHT_ARM, free);
        write(b, PlayerModelPart.LEFT_ARM, Body.LEFT_ARM, free);
        write(b, PlayerModelPart.RIGHT_LEG, Body.RIGHT_LEG, free);
        write(b, PlayerModelPart.LEFT_LEG, Body.LEFT_LEG, free);
        if (body.rootChanged) {
            // the whole body, before the model is flipped: there x is your right, y up, z back
            b.get(PlayerModelPart.BODY)
                    .setX((float) (-body.lift[0] * free)).setY((float) (-body.lift[1] * free)).setZ((float) (body.lift[2] * free))
                    .setXRot((float) (-body.tip[0] * free)).setYRot((float) (-body.tip[1] * free)).setZRot((float) (body.tip[2] * free));
        }
    }

    private void read(ModelPart part, int which) {
        body.set(which, part.x, part.y, part.z, part.xRot, part.yRot, part.zRot);
    }

    private void write(Builder b, PlayerModelPart part, int which, double free) {
        if (!body.didChange(which)) return;
        double[] p = body.part[which];
        PartAnimationData data = b.get(part);
        data.setX((float) p[0]).setY((float) p[1]).setZ((float) p[2]).setXRotShortest((float) p[3]).setYRotShortest((float) p[4]).setZRotShortest((float) p[5]);
        data.multiplier((float) free);
    }
}
