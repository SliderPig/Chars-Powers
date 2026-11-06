package com.example.charpowers.armour.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

// The suit's shape: the normal armour boxes plus raised plates that really stick out: a crest, a chest plate, a back pack,
// shoulders, forearm bands, knee guards and boots. The sheet is tools/make_depth_art.py. Each extra has its own patch of it.
public final class SuitModel {
    private static HumanoidModel<LivingEntity> built;

    private SuitModel() {}

    public static HumanoidModel<LivingEntity> get() {
        if (built == null) built = new HumanoidModel<>(definition().bakeRoot());
        return built;
    }

    // A model of its own (for the flying suit), so its poses never get mixed up with the worn one.
    public static HumanoidModel<LivingEntity> fresh() {
        return new HumanoidModel<>(definition().bakeRoot());
    }

    private static LayerDefinition definition() {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(1f), 0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(0, 32).addBox(-1f, -11.2f, -5.8f, 2f, 3f, 11f), PartPose.ZERO);
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(28, 32).addBox(-3.5f, 0.2f, -3.8f, 7f, 7f, 1f), PartPose.ZERO);
        body.addOrReplaceChild("pack", CubeListBuilder.create().texOffs(46, 32).addBox(-3f, 0.8f, 2.8f, 6f, 8f, 2f), PartPose.ZERO);
        PartDefinition rightArm = root.getChild("right_arm");
        rightArm.addOrReplaceChild("shoulder", CubeListBuilder.create().texOffs(0, 46).addBox(-5f, -3.8f, -3.5f, 6f, 3f, 7f), PartPose.ZERO);
        rightArm.addOrReplaceChild("band", CubeListBuilder.create().texOffs(28, 46).addBox(-3.5f, 6.5f, -2.5f, 5f, 2f, 5f), PartPose.ZERO);
        PartDefinition leftArm = root.getChild("left_arm");
        leftArm.addOrReplaceChild("shoulder", CubeListBuilder.create().texOffs(0, 46).mirror().addBox(-1f, -3.8f, -3.5f, 6f, 3f, 7f), PartPose.ZERO);
        leftArm.addOrReplaceChild("band", CubeListBuilder.create().texOffs(28, 46).mirror().addBox(-1.5f, 6.5f, -2.5f, 5f, 2f, 5f), PartPose.ZERO);
        PartDefinition rightLeg = root.getChild("right_leg");
        rightLeg.addOrReplaceChild("knee", CubeListBuilder.create().texOffs(48, 46).addBox(-2.5f, 3.5f, -3.6f, 5f, 3f, 1f), PartPose.ZERO);
        rightLeg.addOrReplaceChild("boot", CubeListBuilder.create().texOffs(0, 56).addBox(-2.5f, 10f, -3.8f, 5f, 3f, 6f), PartPose.ZERO);
        PartDefinition leftLeg = root.getChild("left_leg");
        leftLeg.addOrReplaceChild("knee", CubeListBuilder.create().texOffs(48, 46).mirror().addBox(-2.5f, 3.5f, -3.6f, 5f, 3f, 1f), PartPose.ZERO);
        leftLeg.addOrReplaceChild("boot", CubeListBuilder.create().texOffs(0, 56).mirror().addBox(-2.5f, 10f, -3.8f, 5f, 3f, 6f), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
