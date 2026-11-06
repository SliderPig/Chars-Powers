package com.example.charpowers.client.anim;

import com.example.charpowers.CharPowers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Something a pose has in its hands: "prop": "book" in player_poses.json. Grows in as the pose fades in, and it's gone with it.
public final class PropLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final BookModel book;

    private PropLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, BookModel book) {
        super(parent);
        this.book = book;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p, float limb, float amount, float partial, float age, float head, float pitch) {
        if (!PlayerAnims.anyActive() || p.isInvisible()) return;
        float open = PlayerAnims.propWeight(p, "book", partial);
        if (open <= 0) return;
        pose.pushPose();
        // held out in front of the chest, the pages tipped up at the reader
        getParentModel().body.translateAndRotate(pose);
        pose.translate(0f, 6f / 16f, -8.5f / 16f);
        pose.mulPose(Axis.XP.rotationDegrees(-25f));
        pose.mulPose(Axis.YP.rotationDegrees(90f));
        pose.mulPose(Axis.ZP.rotationDegrees(-90f));
        pose.scale(open * 0.8f, open * 0.8f, open * 0.8f);
        // the pages lift over slowly
        float turn = age * 0.02f;
        float left = turn % 1f, right = (turn + 0.4f) % 1f;
        book.setupAnim(age, left, right, open * 0.95f);
        VertexConsumer v = EnchantTableRenderer.BOOK_LOCATION.buffer(buffers, RenderType::entitySolid);
        book.renderToBuffer(pose, v, light, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
        pose.popPose();
    }

    @Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        private Setup() {}

        @SubscribeEvent
        @SuppressWarnings({"unchecked", "rawtypes"})
        public static void layers(EntityRenderersEvent.AddLayers e) {
            for (String skin : e.getSkins()) {
                LivingEntityRenderer renderer = e.getSkin(skin);
                if (renderer != null) renderer.addLayer(new PropLayer(renderer, new BookModel(e.getEntityModels().bakeLayer(ModelLayers.BOOK))));
            }
        }
    }
}
