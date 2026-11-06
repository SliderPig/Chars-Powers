package com.example.charpowers.cloning.jobs;

import com.example.charpowers.cloning.CloneEntity;
import com.example.charpowers.cloning.CloneGear;
import com.example.charpowers.cloning.CloneManager;
import com.example.charpowers.cloning.CloneWork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import net.minecraftforge.common.ForgeHooks;

// Takes all the meat from the marked chest and cooks it in the nearest furnace or smoker, using fuel from the chest
// too, and brings the cooked food back. It stands by the furnace while it does.
public class CookJob implements CloneJob {
    private static final int ACT_EVERY = 20, CHUNKS = 2, REACH_SQR = 16, SMOKER_BONUS = 64, STACK = 64;
    private BlockPos furnace;
    private boolean saidNoFuel, saidDone;

    @Override
    public String id() {
        return "cook";
    }

    @Override
    public String describe(CloneEntity c) {
        if (furnace == null) return "Looking for a furnace";
        return (saidDone ? "Done, waiting by " : "Cooking at ") + furnace.toShortString();
    }

    @Override
    public void stop(CloneEntity c) {
        c.mover.stop();
    }

    @Override
    public void tick(CloneEntity c) {
        ServerPlayer host = CloneManager.host(c);
        if (CloneWork.fightBack(c, host)) return;
        if (c.tickCount % ACT_EVERY != 0) return;
        Container chest = CloneWork.chest(c, host);
        if (chest == null) {
            CloneWork.say(c, host, "needs a marked chest to cook from.");
            c.setJob(new FollowJob());
            return;
        }
        AbstractFurnaceBlockEntity f = furnaceNearby(c);
        if (f == null) {
            CloneWork.say(c, host, "can't find a furnace or smoker nearby.");
            c.setJob(new FollowJob());
            return;
        }
        if (furnace.distSqr(c.blockPosition()) > REACH_SQR) {
            c.mover.walkTo(furnace);
            return;
        }
        c.mover.stop();
        work(c, host, chest, f);
    }

    private void work(CloneEntity c, ServerPlayer host, Container chest, AbstractFurnaceBlockEntity f) {
        ItemStack cooked = f.getItem(2);
        if (!cooked.isEmpty()) {
            CloneGear.storeIn(c, chest, cooked.copy());
            f.setItem(2, ItemStack.EMPTY);
            f.awardUsedRecipesAndPopExperience(c);
        }
        ItemStack input = f.getItem(0);
        boolean took = false;
        for (int i = 0; i < chest.getContainerSize() && input.getCount() < STACK; i++) {
            ItemStack s = chest.getItem(i);
            if (!isRawMeat(c, f, s)) continue;
            if (!input.isEmpty() && !ItemStack.isSameItemSameTags(input, s)) continue;
            int count = Math.min(STACK - input.getCount(), s.getCount());
            ItemStack moved = chest.removeItem(i, count);
            if (input.isEmpty()) {
                f.setItem(0, moved);
            } else {
                input.grow(moved.getCount());
                f.setChanged();
            }
            input = f.getItem(0);
            took = true;
        }
        chest.setChanged();
        if (!input.isEmpty() && f.getItem(1).isEmpty() && !hasFire(f)) {
            boolean fuelled = false;
            for (int i = 0; i < chest.getContainerSize() && !fuelled; i++) {
                ItemStack s = chest.getItem(i);
                if (!s.isEmpty() && !s.isDamageableItem() && ForgeHooks.getBurnTime(s, RecipeType.SMELTING) > 0) {
                    f.setItem(1, chest.removeItem(i, s.getCount()));
                    fuelled = true;
                }
            }
            chest.setChanged();
            if (!fuelled && !saidNoFuel) {
                saidNoFuel = true;
                CloneWork.say(c, host, "needs fuel in its chest to cook with.");
            }
        }
        boolean busy = !input.isEmpty() || took;
        if (busy) {
            saidDone = false;
        } else if (!saidDone) {
            saidDone = true;
            saidNoFuel = false;
            CloneWork.say(c, host, "has cooked everything in its chest.");
        }
    }

    private static boolean hasFire(AbstractFurnaceBlockEntity f) {
        return f.getBlockState().hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)
                && f.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT);
    }

    private boolean isRawMeat(CloneEntity c, AbstractFurnaceBlockEntity f, ItemStack s) {
        if (s.isEmpty() || !s.isEdible()) return false;
        FoodProperties food = s.getFoodProperties(c);
        if (food == null || !(food.isMeat() || s.is(ItemTags.FISHES))) return false;
        SimpleContainer one = new SimpleContainer(s);
        if (f instanceof SmokerBlockEntity) return c.level().getRecipeManager().getRecipeFor(RecipeType.SMOKING, one, c.level()).isPresent();
        return c.level().getRecipeManager().getRecipeFor(RecipeType.SMELTING, one, c.level()).isPresent();
    }

    private AbstractFurnaceBlockEntity furnaceNearby(CloneEntity c) {
        ServerLevel level = c.serverLevel();
        if (furnace != null && level.getBlockEntity(furnace) instanceof AbstractFurnaceBlockEntity f) return f;
        ChunkPos here = c.chunkPosition();
        AbstractFurnaceBlockEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (int x = here.x - CHUNKS; x <= here.x + CHUNKS; x++) {
            for (int z = here.z - CHUNKS; z <= here.z + CHUNKS; z++) {
                if (!level.hasChunk(x, z)) continue;
                for (BlockEntity be : level.getChunk(x, z).getBlockEntities().values()) {
                    if (!(be instanceof AbstractFurnaceBlockEntity f)) continue;
                    // a smoker cooks twice as fast, so it's worth a few steps more
                    double score = be.getBlockPos().distSqr(c.blockPosition()) - (be instanceof SmokerBlockEntity ? SMOKER_BONUS : 0);
                    if (be.getClass().getSimpleName().contains("Blast")) continue;
                    if (score < bestScore) {
                        bestScore = score;
                        best = f;
                    }
                }
            }
        }
        furnace = best == null ? null : best.getBlockPos();
        return best;
    }
}
