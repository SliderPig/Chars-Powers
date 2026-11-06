package com.example.charpowers.cloning.client;

import com.example.charpowers.cloning.DogPileMob;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.UUID;

// Draws a Dog Pile fighter with its owner's real skin and arm shape, armour and held items included.
public class DogPileRenderer extends HumanoidMobRenderer<DogPileMob, PlayerModel<DogPileMob>> {
    private final PlayerModel<DogPileMob> wide;
    private final PlayerModel<DogPileMob> slim;

    public DogPileRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        wide = getModel();
        slim = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidArmorModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
    }

    @Nullable
    private static PlayerInfo info(DogPileMob mob) {
        UUID id = mob.hostId();
        ClientPacketListener net = Minecraft.getInstance().getConnection();
        return id == null || net == null ? null : net.getPlayerInfo(id);
    }

    @Override
    public void render(DogPileMob mob, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        PlayerInfo info = info(mob);
        model = info != null && "slim".equals(info.getModelName()) ? slim : wide;
        super.render(mob, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(DogPileMob mob) {
        PlayerInfo info = info(mob);
        if (info != null) return info.getSkinLocation();
        UUID id = mob.hostId();
        return id == null ? DefaultPlayerSkin.getDefaultSkin() : DefaultPlayerSkin.getDefaultSkin(id);
    }
}
