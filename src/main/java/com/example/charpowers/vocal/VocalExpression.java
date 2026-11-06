package com.example.charpowers.vocal;

import com.example.charpowers.own.Setting;
import com.example.charpowers.CharPowers;
import com.example.charpowers.hidden.HiddenRuntime;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.own.OwnPower;
import com.example.charpowers.vocal.model.WordRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.PowerManager;
import net.threetag.palladium.power.ability.AbilityUtil;
import net.threetag.palladium.power.energybar.EnergyBar;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Type words in chat and they appear in the world, solid. Speak (a toggle) decides whether your chat becomes words, so
// you can still talk normally. The words themselves are in Words.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class VocalExpression extends OwnPower {
    public static final VocalExpression POWER = new VocalExpression();
    private static final String MESSAGES = "messages", BAR = "breath";
    private static final int BREATH_PER_SECOND = 2;
    private static final Map<UUID, BlockPos> CROSSING = new HashMap<>();

    private VocalExpression() {
        super(Mastery.VOCAL_EXPRESSION, Items.WRITABLE_BOOK);
    }

    @Override
    public String unlockMessage() { return "That was your thousandth message. All that talking finally came to something."; }

    @Override
    public String awakenedMessage() { return "You crossed on nothing but your own words. Now they do what they say: BURN, ICE, BOOM."; }

    private static EnergyBar breath(ServerPlayer p) {
        return PowerManager.getPowerHandler(p).map(h -> {
            var holder = h.getPowerHolders().get(POWER.id);
            return holder == null ? null : holder.getEnergyBars().get(BAR);
        }).orElse(null);
    }

    @Override
    public void everySecond(ServerPlayer p) {
        EnergyBar bar = breath(p);
        if (bar != null) bar.add(BREATH_PER_SECOND);
        if (Words.hasWordOut(p)) Mastery.add(p, id, 1);
    }

    // Counted for everyone, off the chat thread. Words only for owners with Speak on, and those don't go to chat.
    @SubscribeEvent
    public static void onChat(ServerChatEvent e) {
        ServerPlayer p = e.getPlayer();
        MinecraftServer server = p.getServer();
        if (server == null) return;
        String text = e.getRawText();
        boolean speaking = POWER.owns(p) && AbilityUtil.isEnabled(p, POWER.id, "speak") && !HiddenRuntime.switchedOff(p);
        if (speaking) e.setCanceled(true);
        server.execute(() -> {
            if (speaking) say(p, text);
            else if (!POWER.owns(p)) countMessage(p);
        });
    }

    private static void countMessage(ServerPlayer p) {
        int count = POWER.data(p).getInt(MESSAGES) + 1;
        POWER.data(p).putInt(MESSAGES, count);
        if (count >= Setting.VOCAL_MESSAGES.i()) POWER.unlock(p);
    }

    private static void say(ServerPlayer p, String text) {
        int level = POWER.mastery(p);
        String word = WordRules.clean(text, WordRules.maxLetters(level));
        if (word.isEmpty()) return;
        com.example.charpowers.anim.AnimNet.play(p, "shout");
        EnergyBar bar = breath(p);
        int cost = WordRules.cost(WordRules.letters(word), level);
        if (bar != null) {
            if (bar.get() < cost) {
                Words.noRoom(p);
                return;
            }
            bar.add(-cost);
        }
        Words.shout(p, word, level, p.isCrouching());
    }

    // The quest: 200 blocks, only ever touching the ground on your own words. Checked 5 times a second, only on the quest.
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || p.tickCount % 4 != 0 || !POWER.owns(p) || !POWER.questReady(p)) return;
        if (p.getAbilities().flying || p.isFallFlying() || p.isPassenger()) {
            CROSSING.remove(p.getUUID());
            return;
        }
        if (!p.onGround()) return;
        boolean onWord = p.level().getBlockState(p.blockPosition().below()).is(Words.WORD.get());
        if (!onWord) {
            CROSSING.remove(p.getUUID());
            return;
        }
        BlockPos start = CROSSING.computeIfAbsent(p.getUUID(), u -> p.blockPosition());
        double dx = p.getX() - start.getX(), dz = p.getZ() - start.getZ();
        if (dx * dx + dz * dz >= WordRules.GAP_TO_CROSS * WordRules.GAP_TO_CROSS) {
            CROSSING.remove(p.getUUID());
            POWER.awaken(p);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        CROSSING.remove(e.getEntity().getUUID());
    }
}
