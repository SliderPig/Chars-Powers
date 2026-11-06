package com.example.charpowers.game.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.game.mob.GateBoss;
import com.example.charpowers.game.mob.GateKind;
import com.example.charpowers.game.mob.GateMob;
import com.example.charpowers.game.mob.GateMobs;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

// How the gate creatures look: each has its own cubes (assets/charpowers/gate_models/<kind>.json, made by tools/make_gate_mobs.py)
// on the usual humanoid skeleton, its own picture, and glowing eyes.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GateRenderers {
    private static final Map<GateKind, Float> SCALE = new EnumMap<>(GateKind.class);

    private GateRenderers() {}

    private static ModelLayerLocation layer(GateKind kind) {
        return new ModelLayerLocation(new ResourceLocation(CharPowers.MODID, kind.id), "main");
    }

    private static JsonObject read(GateKind kind) {
        try (var in = GateRenderers.class.getResourceAsStream("/assets/charpowers/gate_models/" + kind.id + ".json")) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            throw new IllegalStateException("No model for " + kind.id, e);
        }
    }

    private static float f(JsonArray a, int i) {
        return a.get(i).getAsFloat();
    }

    private static void addParts(PartDefinition parent, JsonObject parts) {
        for (Map.Entry<String, JsonElement> entry : parts.entrySet()) {
            JsonObject part = entry.getValue().getAsJsonObject();
            CubeListBuilder cubes = CubeListBuilder.create();
            for (JsonElement ce : part.getAsJsonArray("cubes")) {
                JsonObject c = ce.getAsJsonObject();
                JsonArray tex = c.getAsJsonArray("tex"), from = c.getAsJsonArray("from"), size = c.getAsJsonArray("size");
                cubes.texOffs(tex.get(0).getAsInt(), tex.get(1).getAsInt()).addBox(f(from, 0), f(from, 1), f(from, 2), f(size, 0), f(size, 1), f(size, 2));
            }
            JsonArray pivot = part.getAsJsonArray("pivot");
            JsonArray rot = part.has("rot") ? part.getAsJsonArray("rot") : null;
            PartPose pose = rot == null ? PartPose.offset(f(pivot, 0), f(pivot, 1), f(pivot, 2))
                    : PartPose.offsetAndRotation(f(pivot, 0), f(pivot, 1), f(pivot, 2), f(rot, 0), f(rot, 1), f(rot, 2));
            PartDefinition made = parent.addOrReplaceChild(entry.getKey(), cubes, pose);
            if (part.has("children")) addParts(made, part.getAsJsonObject("children"));
        }
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        for (GateKind kind : GateKind.values()) {
            JsonObject json = read(kind);
            MeshDefinition mesh = new MeshDefinition();
            addParts(mesh.getRoot(), json.getAsJsonObject("parts"));
            JsonArray tex = json.getAsJsonArray("tex");
            SCALE.put(kind, json.get("scale").getAsFloat());
            e.registerLayerDefinition(layer(kind), () -> LayerDefinition.create(mesh, tex.get(0).getAsInt(), tex.get(1).getAsInt()));
        }
    }

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        for (GateKind kind : GateKind.values()) {
            e.registerEntityRenderer((net.minecraft.world.entity.EntityType) GateMobs.mobType(kind), (EntityRendererProvider) ctx -> new Renderer(ctx, kind));
        }
    }

    // The skeleton: arms go up while a creature is getting ready for something, and elves hold the bow up.
    static final class Model extends HumanoidModel<Mob> {
        private final GateKind kind;

        Model(ModelPart root, GateKind kind) {
            super(root);
            this.kind = kind;
        }

        @Override
        public void prepareMobModel(Mob mob, float limbSwing, float limbSwingAmount, float partial) {
            rightArmPose = ArmPose.EMPTY;
            leftArmPose = ArmPose.EMPTY;
            if (kind == GateKind.ELF && mob.isAggressive() && mob.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.BOW)) rightArmPose = ArmPose.BOW_AND_ARROW;
            super.prepareMobModel(mob, limbSwing, limbSwingAmount, partial);
        }

        @Override
        public void setupAnim(Mob mob, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch) {
            super.setupAnim(mob, limbSwing, limbSwingAmount, age, headYaw, headPitch);
            // the humanoid skeleton puts the head, arms and legs back at the standard height every frame; the beasts, spiders and other odd shapes need theirs where they were built
            for (ModelPart part : new ModelPart[]{head, body, rightArm, leftArm, rightLeg, leftLeg}) {
                var home = part.getInitialPose();
                part.setPos(home.x, home.y, home.z);
            }
            boolean bare = mob.getMainHandItem().isEmpty();
            int tell = mob instanceof GateMob m ? m.tell() : mob instanceof GateBoss b ? b.tell() : 0;
            if (tell > 0) {
                float shake = (float) Math.sin(age * 1.7f) * 0.05f;
                rightArm.xRot = -2.7f + shake;
                leftArm.xRot = -2.7f - shake;
                rightArm.zRot = 0.15f;
                leftArm.zRot = -0.15f;
            } else if (bare && mob.isAggressive() && kind != GateKind.ELF_MAGE) {
                AnimationUtils.animateZombieArms(leftArm, rightArm, true, attackTime, age);
            }
        }
    }

    static final class Renderer extends MobRenderer<Mob, Model> {
        private final ResourceLocation texture;
        private final float scale;

        Renderer(EntityRendererProvider.Context ctx, GateKind kind) {
            super(ctx, new Model(ctx.bakeLayer(layer(kind)), kind), kind.width * 0.55f);
            this.texture = new ResourceLocation(CharPowers.MODID, "textures/entity/gate/" + kind.id + ".png");
            this.scale = SCALE.getOrDefault(kind, 1f);
            ResourceLocation glow = new ResourceLocation(CharPowers.MODID, "textures/entity/gate/" + kind.id + "_glow.png");
            addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
            addLayer(new RenderLayer<>(this) {
                @Override
                public void render(PoseStack pose, MultiBufferSource buffers, int light, Mob mob, float a, float b, float c, float d, float e, float f) {
                    VertexConsumer vc = buffers.getBuffer(RenderType.eyes(glow));
                    getParentModel().renderToBuffer(pose, vc, 0xF000F0, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
                }
            });
        }

        @Override
        protected void scale(Mob mob, PoseStack pose, float partial) {
            pose.scale(scale, scale, scale);
        }

        @Override
        public ResourceLocation getTextureLocation(Mob mob) {
            return texture;
        }
    }
}
