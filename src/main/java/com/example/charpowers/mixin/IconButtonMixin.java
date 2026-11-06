package com.example.charpowers.mixin;

import com.example.charpowers.client.Soft;
import com.example.charpowers.client.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.threetag.palladium.client.screen.components.IconButton;
import net.threetag.palladium.client.screen.power.PowersScreen;
import net.threetag.palladium.util.context.DataContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Palladium's little power button in the inventory: a soft round one instead of a square. m_87963_ = renderWidget.
@Mixin(value = IconButton.class, remap = false)
public abstract class IconButtonMixin {
    @Inject(method = "m_87963_", at = @At("HEAD"), cancellable = true)
    private void charpowers$draw(GuiGraphics g, int mouseX, int mouseY, float partial, CallbackInfo ci) {
        IconButton self = (IconButton) (Object) this;
        if (!(self instanceof PowersScreen.RotatingIconButton)) return;
        ci.cancel();
        int x = self.getX(), y = self.getY(), w = self.getWidth(), h = self.getHeight();
        boolean over = self.isHovered();
        float cx = x + w / 2f, cy = y + h / 2f, r = Math.min(w, h) / 2f;
        g.drawManaged(() -> {
            if (over) Soft.shadow(g, cx - r, cy - r, cx + r, cy + r, r, 5f, Ui.PINK_GLOW, false);
            Soft.shadow(g, cx - r, cy - r + 1, cx + r, cy + r + 1, r, 3f, 0x50000000, false);
            Soft.round(g, cx - r, cy - r, cx + r, cy + r, r, over ? 0xFF5A4270 : 0xFF3E2F4D, over ? 0xFF49355C : 0xFF2A2035);
            Soft.ring(g, cx - r, cy - r, cx + r, cy + r, r, 1f, over ? 0xE0FF88C8 : 0x90FF88C8, over ? 0x90FF88C8 : 0x40FF88C8);
        });
        Minecraft mc = Minecraft.getInstance();
        self.getIcon().draw(mc, g, mc.player != null ? DataContext.forEntity(mc.player) : DataContext.create(), x + (w - 16) / 2, y + (h - 16) / 2);
        g.flush();
    }
}
