package com.example.charpowers.mixin;

import com.example.charpowers.client.HiddenPowersTab;
import com.example.charpowers.client.power.PowerMenu;
import com.example.charpowers.hidden.HiddenGrant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.threetag.palladium.client.screen.power.PowerTab;
import net.threetag.palladium.client.screen.power.PowersScreen;
import net.threetag.palladium.power.IPowerHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

// After Palladium builds its power menu tabs, swap the plain tab of our "Hidden Powers" power
// for our own tab (collection + the 5 key and 10 always-on slots). The drawing of the whole menu goes to client/power/PowerMenu.
// remap = false and the SRG name m_7856_ (= Screen.init) because Palladium's jar is already SRG-named.
@Mixin(value = PowersScreen.class, remap = false)
public abstract class PowersScreenMixin {
    @Shadow @Final private List<PowerTab> tabs;
    @Shadow public PowerTab selectedTab;

    @Inject(method = "m_6375_", at = @At("HEAD"), cancellable = true)
    private void charpowers$panelClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (PowerMenu.click((PowersScreen) (Object) this, mouseX, mouseY, button)) cir.setReturnValue(true);
    }

    // the card, the side cards and the tabs go down before the tab paints its contents on them
    @Inject(method = "renderInside", at = @At("HEAD"))
    private void charpowers$back(GuiGraphics g, int mouseX, int mouseY, int x, int y, float partial, CallbackInfo ci) {
        PowerMenu.back((PowersScreen) (Object) this, g, x, y, mouseX, mouseY, partial);
    }

    @Inject(method = "renderWindow", at = @At("HEAD"), cancellable = true)
    private void charpowers$window(GuiGraphics g, int x, int y, CallbackInfo ci) {
        PowerMenu.window((PowersScreen) (Object) this, g, x, y);
        ci.cancel();
    }

    @Inject(method = "renderTooltips", at = @At("HEAD"), cancellable = true)
    private void charpowers$tooltips(GuiGraphics g, int mouseX, int mouseY, int x, int y, float partial, CallbackInfo ci) {
        PowerMenu.tooltips((PowersScreen) (Object) this, g, mouseX, mouseY, x, y, partial);
        ci.cancel();
    }

    @Inject(method = "m_7856_", at = @At("TAIL"))
    private void charpowers$useHiddenTab(CallbackInfo ci) {
        PowersScreen self = (PowersScreen) (Object) this;
        for (int i = 0; i < tabs.size(); i++) {
            PowerTab tab = tabs.get(i);
            PowerTabAccessor a = (PowerTabAccessor) tab;
            IPowerHolder holder = a.charpowers$holder();
            if (holder == null || holder.getPower() == null || !HiddenGrant.POWER.equals(holder.getPower().getId())) continue;
            HiddenPowersTab mine = new HiddenPowersTab(Minecraft.getInstance(), self, a.charpowers$type(), a.charpowers$index(), holder);
            tabs.set(i, mine);
            if (selectedTab == tab) selectedTab = mine;
        }
        PowerMenu.init(self);
    }
}
