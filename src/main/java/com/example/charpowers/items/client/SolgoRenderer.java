package com.example.charpowers.items.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.items.Solgo;
import com.example.charpowers.items.model.SolgoRules.Move;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3d;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

// How Solgo looks and moves beyond his animations: his head tilts and follows you with sudden wrong little jerks, and he flickers when he vanishes. His blade leaves a trail.
public final class SolgoRenderer extends GeoEntityRenderer<Solgo> {
    private final double[] base = new double[3], tip = new double[3];

    public SolgoRenderer(EntityRendererProvider.Context c) {
        super(c, new SolgoModel());
        shadowRadius = 1.1f;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public RenderType getRenderType(Solgo e, ResourceLocation texture, MultiBufferSource buffers, float partial) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public void render(Solgo e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        // flickering: he isn't quite there when he vanishes or arrives
        Move m = e.move();
        if (m == Move.VANISH && e.tickCount % 2 == 0 || m == Move.ARRIVE && e.tickCount % 2 == 0 && e.tickCount % 18 < 8) return;
        super.render(e, yaw, partial, pose, buffers, light);
        if (m == Move.SLASH || m == Move.LUNGE) {
            GeoBone tipBone = getGeoModel().getBone("blade_tip").orElse(null);
            GeoBone baseBone = getGeoModel().getBone("blade_base").orElse(null);
            if (tipBone != null && baseBone != null) {
                Vector3d t = tipBone.getWorldPosition(), b = baseBone.getWorldPosition();
                base[0] = b.x;
                base[1] = b.y;
                base[2] = b.z;
                tip[0] = t.x;
                tip[1] = t.y;
                tip[2] = t.z;
                Trails.sample(e.getId(), TrailStyle.SOLGO, base, tip, SwingTrack.now(partial));
            }
        } else {
            Trails.cut(e.getId());
        }
    }

    @Override
    protected float getDeathMaxRotation(Solgo e) {
        return 0;
    }

    @Override
    public int getPackedOverlay(Solgo e, float u, float partial) {
        return e.deathTime > 0 ? OverlayTexture.NO_OVERLAY : super.getPackedOverlay(e, u, partial);
    }

    private static final class SolgoModel extends GeoModel<Solgo> {
        private static final ResourceLocation GEO = new ResourceLocation(CharPowers.MODID, "geo/solgo.geo.json");
        private static final ResourceLocation TEXTURE = new ResourceLocation(CharPowers.MODID, "textures/entity/solgo.png");
        private static final ResourceLocation ANIM = new ResourceLocation(CharPowers.MODID, "animations/solgo.animation.json");

        @Override
        public ResourceLocation getModelResource(Solgo e) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(Solgo e) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(Solgo e) {
            return ANIM;
        }

        @Override
        public void setCustomAnimations(Solgo e, long id, AnimationState<Solgo> state) {
            getBone("blade_tip").ifPresent(b -> b.setTrackingMatrices(true));
            getBone("blade_base").ifPresent(b -> b.setTrackingMatrices(true));
            GeoBone head = getBone("head").orElse(null);
            if (head == null) return;
            EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
            float age = e.tickCount + state.getPartialTick();
            // a slow tilt, and now and then it snaps somewhere for a moment
            float tilt = Mth.sin(age * 0.05f) * 0.32f;
            long beat = (long) (age / 37f);
            long mix = beat * 2654435761L;
            boolean jerking = (mix & 3L) == 0 && (age % 37f) < 7f;
            float jerkZ = jerking ? ((mix >> 8 & 1L) == 0 ? 0.6f : -0.6f) : 0f;
            float jerkY = jerking ? ((mix >> 9 & 1L) == 0 ? 0.25f : -0.25f) : 0f;
            float yaw = data == null ? 0 : data.netHeadYaw() * Mth.DEG_TO_RAD;
            float pitch = data == null ? 0 : data.headPitch() * Mth.DEG_TO_RAD;
            head.setRotZ(head.getInitialSnapshot().getRotZ() + tilt + jerkZ);
            head.setRotY(head.getInitialSnapshot().getRotY() + Mth.clamp(yaw, -1.2f, 1.2f) * -1f + jerkY);
            head.setRotX(head.getInitialSnapshot().getRotX() + Mth.clamp(pitch, -0.7f, 0.7f) * -1f);
        }
    }
}
