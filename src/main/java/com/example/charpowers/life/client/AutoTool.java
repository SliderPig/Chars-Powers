package com.example.charpowers.life.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.mastery.Mastery;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.ability.AbilityUtil;

// When you start hitting a block, the best tool on your hotbar comes to your hand. Only if something beats what you hold.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class AutoTool {
    private AutoTool() {}

    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock e) {
        if (!e.getLevel().isClientSide() || !(e.getEntity() instanceof LocalPlayer p) || p.isCreative()) return;
        if (!AbilityUtil.isEnabled(p, Mastery.LIFE_ENJOYER, "auto_tool")) return;
        BlockState state = e.getLevel().getBlockState(e.getPos());
        if (state.getDestroySpeed(e.getLevel(), e.getPos()) == 0) return;
        Inventory inv = p.getInventory();
        float best = inv.getItem(inv.selected).getDestroySpeed(state);
        int slot = -1;
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            float speed = inv.getItem(i).getDestroySpeed(state);
            if (speed > best && speed > 1f) {
                best = speed;
                slot = i;
            }
        }
        if (slot >= 0) inv.selected = slot;
    }
}
