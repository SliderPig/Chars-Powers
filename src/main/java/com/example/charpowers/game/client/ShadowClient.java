package com.example.charpowers.game.client;

import com.example.charpowers.CharPowers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Set;

// How a shadow looks: the creature's own model darkened to black-purple, with two glowing eyes, and a wisp of dark dust rising.
// Works for every living creature (a layer on every renderer), so the custom gate creatures and the vanilla ones are treated alike.
public final class ShadowClient {
    private static volatile Set<Integer> ids = Set.of();
    private static final DustParticleOptions WISP = new DustParticleOptions(new Vector3f(0.22f, 0.04f, 0.34f), 1.1f);

    private ShadowClient() {}

    public static void set(int[] list) {
        Set<Integer> now = new java.util.HashSet<>();
        for (int i : list) now.add(i);
        ids = now;
    }

    public static boolean isShadow(int entityId) {
        return ids.contains(entityId);
    }

    // for the real-game check
    public static void setForTest(int... list) {
        set(list);
    }

    public static final class ShadowLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
        public ShadowLayer(RenderLayerParent<T, M> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount, float partial, float age, float headYaw, float headPitch) {
            if (!isShadow(entity.getId()) || entity.isInvisible()) return;
            ResourceLocation tex = getTextureLocation(entity);
            VertexConsumer body = buffers.getBuffer(RenderType.entityTranslucent(tex));
            getParentModel().renderToBuffer(pose, body, light, LivingEntityRenderer.getOverlayCoords(entity, 0f), 0.16f, 0.04f, 0.26f, 0.82f);
            // two eyes on the front of the head, bright whatever the light
            float w = Math.max(0.3f, entity.getBbWidth());
            float eyeY = 1.501f - entity.getEyeHeight();
            float spread = 0.13f * (w / 0.6f), front = -(0.255f * (w / 0.6f)) - 0.01f, size = 0.045f * Math.max(1f, w / 0.6f);
            pose.pushPose();
            pose.translate(0f, eyeY, 0f);
            pose.mulPose(new Quaternionf().rotationZYX(0f, headYaw * ((float) Math.PI / 180f), headPitch * ((float) Math.PI / 180f)));
            VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
            for (int side = -1; side <= 1; side += 2) cube(pose.last().pose(), glow, side * spread, 0f, front, size, 0.95f, 0.55f, 1.0f);
            pose.popPose();
        }

        private static void cube(Matrix4f m, VertexConsumer v, float cx, float cy, float cz, float s, float r, float g, float b) {
            float x0 = cx - s, x1 = cx + s, y0 = cy - s, y1 = cy + s, z0 = cz - s, z1 = cz + s;
            quad(m, v, r, g, b, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
            quad(m, v, r, g, b, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1);
            quad(m, v, r, g, b, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1);
            quad(m, v, r, g, b, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
            quad(m, v, r, g, b, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0);
            quad(m, v, r, g, b, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
        }

        private static void quad(Matrix4f m, VertexConsumer v, float r, float g, float b, float... p) {
            // both ways round, so it shows whichever side the culling would hide
            for (int i = 0; i < 12; i += 3) v.vertex(m, p[i], p[i + 1], p[i + 2]).color(r, g, b, 0.95f).endVertex();
            for (int i = 9; i >= 0; i -= 3) v.vertex(m, p[i], p[i + 1], p[i + 2]).color(r, g, b, 0.95f).endVertex();
        }
    }

    @Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        private Setup() {}

        @SuppressWarnings({"unchecked", "rawtypes"})
        @SubscribeEvent
        public static void layers(EntityRenderersEvent.AddLayers e) {
            for (EntityType<?> type : net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValues()) {
                try {
                    Object r = e.getRenderer((EntityType) type);
                    if (r instanceof LivingEntityRenderer ler) ler.addLayer(new ShadowLayer(ler));
                } catch (RuntimeException ignored) {
                    // a renderer that does not take layers is simply left plain
                }
            }
        }
    }

    @Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
    public static final class Events {
        private Events() {}

        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent e) {
            Minecraft mc = Minecraft.getInstance();
            if (e.phase != TickEvent.Phase.END || ids.isEmpty() || mc.level == null || mc.isPaused() || mc.level.getGameTime() % 5 != 0) return;
            for (int id : ids) {
                var en = mc.level.getEntity(id);
                if (en == null || !en.isAlive()) continue;
                mc.level.addParticle(WISP, en.getX() + (mc.level.random.nextDouble() - 0.5) * en.getBbWidth(), en.getY() + en.getBbHeight() * mc.level.random.nextDouble(),
                        en.getZ() + (mc.level.random.nextDouble() - 0.5) * en.getBbWidth(), 0, 0.03, 0);
            }
        }
    }
}
