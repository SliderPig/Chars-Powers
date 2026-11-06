package com.example.charpowers.lantern;

import com.example.charpowers.own.OwnPower;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// The Power Battery. Use it to be reminded of your oath, crouch and use it to change the oath.
public class LanternItem extends Item {
    public LanternItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer p && GreenLantern.POWER.owns(p)) {
            if (p.isShiftKeyDown()) GreenLantern.POWER.askOath(p);
            else OwnPower.tell(p, "Your oath: \"" + GreenLantern.POWER.oathOf(p) + "\"");
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
