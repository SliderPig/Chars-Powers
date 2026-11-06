package com.example.charpowers.client;

import com.example.charpowers.CharPowers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

// Little lines in the top-left corner from your hidden powers: Light Meter, Health Peek, Horse Whisperer,
// and whatever the server sends (Nine Lives' lives, Everything's power right now, the Rage Meter...).
// Also Pumpkin Vision (no pumpkin blur over your screen).
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class ClientHud {
    private static final int PINK = 0xFFFF88C8;

    @SubscribeEvent
    public static void onDraw(RenderGuiEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || mc.options.renderDebug || com.example.charpowers.client.ui.HudStack.menuOpen()) return;
        List<String> lines = new ArrayList<>(ClientHidden.hudLines());

        // #153 Light Meter: block light 0 = monsters can spawn here
        if (ClientHidden.has("light_meter")) {
            BlockPos at = mc.player.blockPosition();
            int block = mc.level.getBrightness(LightLayer.BLOCK, at), sky = mc.level.getBrightness(LightLayer.SKY, at);
            lines.add("Light: " + block + " (sky " + sky + ")" + (block == 0 ? " - mobs can spawn" : " - safe"));
        }
        Entity looked = mc.hitResult instanceof EntityHitResult h ? h.getEntity() : null;
        // #187 Health Peek
        if (ClientHidden.has("health_peek") && looked instanceof LivingEntity le && !(looked instanceof ArmorStand)) {
            lines.add(le.getDisplayName().getString() + ": " + half(le.getHealth()) + " / " + half(le.getMaxHealth()) + " hearts");
        }
        // #154 Horse Whisperer: speed in blocks a second, jump in blocks
        if (ClientHidden.has("horse_whisperer") && looked instanceof AbstractHorse horse) {
            double speed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED) * 42.16;
            double j = horse.getCustomJump();
            double height = -0.1817584952 * j * j * j + 3.689713992 * j * j + 2.128599134 * j - 0.343930367;
            lines.add(String.format("Horse: speed %.1f blocks/s, jump %.1f blocks", speed, height));
        }

        GuiGraphics g = e.getGuiGraphics();
        if (lines.isEmpty()) return;
        g.drawManaged(() -> {
            for (String line : lines) {
                float h = 13, y = com.example.charpowers.client.ui.HudStack.take(h);
                com.example.charpowers.client.ui.HudStack.chip(g, 6, y, com.example.charpowers.client.ui.Ui.width(line, com.example.charpowers.client.ui.Ui.S) + 12, h, 0xD0241A2C);
                com.example.charpowers.client.ui.Ui.text(g, line, 12, com.example.charpowers.client.ui.Ui.middle(y + h / 2f, com.example.charpowers.client.ui.Ui.S), PINK, com.example.charpowers.client.ui.Ui.S);
            }
        });
    }

    private static String half(float hp) {
        float h = Math.round(hp) / 2f;
        return h == (int) h ? Integer.toString((int) h) : Float.toString(h);
    }

    // #156 Pumpkin Vision: no pumpkin blur over your screen
    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Pre e) {
        if (e.getOverlay() != VanillaGuiOverlay.HELMET.type()) return;
        var p = Minecraft.getInstance().player;
        if (p != null && p.getInventory().getArmor(3).is(Items.CARVED_PUMPKIN) && ClientHidden.has("pumpkin_vision")) e.setCanceled(true);
    }

    private ClientHud() {}
}
