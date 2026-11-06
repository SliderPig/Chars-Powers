package com.example.charpowers.vocal;

import com.example.charpowers.CharPowers;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.own.OwnPower;
import com.example.charpowers.vocal.model.WordRules;
import com.example.charpowers.sound.PowerSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Words flying and words standing. A flying word is floating text. Where it stops, it turns into solid temporary
// blocks, a bit each tick, and they go again when its time is up. Nothing runs when no word is out.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class Words {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, CharPowers.MODID);
    public static final RegistryObject<Block> WORD = BLOCKS.register("word", WordBlock::new);

    private static final double SPEED = 2.0;
    private static final int MAX_FLIGHT = 40, PLACE_PER_TICK = 200, REMOVE_PER_TICK = 200;

    private static final List<Flying> FLYING = new ArrayList<>();
    private static final List<Placing> PLACING = new ArrayList<>();
    private static final Map<ServerLevel, WordData> LIVE = new HashMap<>();

    public static final class WordBlock extends Block {
        public static final BooleanProperty INK = BooleanProperty.create("ink");

        WordBlock() {
            super(BlockBehaviour.Properties.of().strength(-1f, 3600000f).noLootTable()
                    .isValidSpawn((s, l, p, t) -> false).pushReaction(PushReaction.BLOCK));
            registerDefaultState(stateDefinition.any().setValue(INK, true));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(INK);
        }
    }

    private static final class Flying {
        final UUID owner;
        final String word;
        final boolean flat;
        final Display.TextDisplay text;
        final Direction ahead;
        final Vec3 velocity;
        final BlockPos feet;
        final Set<Integer> hit = new HashSet<>();
        final int level;
        final int maxTicks;
        Vec3 at;
        int ticks;

        Flying(ServerPlayer p, String word, boolean flat, Display.TextDisplay text, Vec3 at, Vec3 velocity, int level, int maxTicks) {
            this.owner = p.getUUID();
            this.word = word;
            this.flat = flat;
            this.text = text;
            this.at = at;
            this.velocity = velocity;
            this.ahead = p.getDirection();
            this.feet = p.blockPosition();
            this.level = level;
            this.maxTicks = maxTicks;
        }
    }

    private record Placing(ServerLevel level, WordData.Word word, Deque<Long> spots, Deque<Boolean> inks) {}

    private Words() {}

    public static void register(net.minecraftforge.eventbus.api.IEventBus modBus) {
        BLOCKS.register(modBus);
    }

    private static WordData data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(WordData::load, WordData::new, WordData.NAME);
    }

    // Standing words left in a world from last time: keep timing them.
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        WordData data = data(level);
        if (!data.words.isEmpty()) LIVE.put(level, data);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload e) {
        if (e.getLevel() instanceof ServerLevel level) LIVE.remove(level);
    }

    // Says the word. Off the chat thread already, so it's safe to touch the world.
    public static void shout(ServerPlayer p, String typed, int level, boolean flat) {
        String word = WordRules.clean(typed, WordRules.maxLetters(level));
        if (word.isEmpty()) return;
        ServerLevel world = p.serverLevel();
        endOldest(p, world, level);
        Vec3 at = p.getEyePosition().add(p.getLookAngle().scale(1.5));
        Vec3 look = flat ? Vec3.atLowerCornerOf(p.getDirection().getNormal()) : p.getLookAngle();
        Display.TextDisplay text = EntityType.TEXT_DISPLAY.create(world);
        if (text == null) return;
        text.setPos(at.x, at.y, at.z);
        CompoundTag tag = text.saveWithoutId(new CompoundTag());
        tag.putString("text", Component.Serializer.toJson(Component.literal(word).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE, net.minecraft.ChatFormatting.BOLD)));
        tag.putString("billboard", "center");
        CompoundTag transform = new CompoundTag();
        transform.put("left_rotation", floats(0, 0, 0, 1));
        transform.put("right_rotation", floats(0, 0, 0, 1));
        transform.put("translation", floats(0, 0, 0));
        transform.put("scale", floats(4, 4, 4));
        tag.put("transformation", transform);
        text.load(tag);
        world.addFreshEntity(text);
        int width = WordRules.width(word);
        int ticks = flat ? Math.max(4, (int) (width / SPEED)) : MAX_FLIGHT;
        FLYING.add(new Flying(p, word, flat, text, at, look.scale(SPEED), level, ticks));
    }

    private static ListTag floats(float... values) {
        ListTag list = new ListTag();
        for (float v : values) list.add(FloatTag.valueOf(v));
        return list;
    }

    // Only so many words at once: a new one sends your oldest away.
    private static void endOldest(ServerPlayer p, ServerLevel world, int level) {
        WordData data = data(world);
        int mine = 0;
        WordData.Word oldest = null;
        for (WordData.Word w : data.words) {
            if (!w.owner.equals(p.getUUID())) continue;
            mine++;
            if (oldest == null || w.expires < oldest.expires) oldest = w;
        }
        if (oldest != null && mine >= WordRules.wordsAtOnce(level)) oldest.expires = 0;
    }

    public static boolean hasWordOut(ServerPlayer p) {
        for (Flying f : FLYING) {
            if (f.owner.equals(p.getUUID())) return true;
        }
        for (WordData data : LIVE.values()) {
            for (WordData.Word w : data.words) {
                if (w.owner.equals(p.getUUID())) return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!FLYING.isEmpty()) tickFlying(e);
        if (!PLACING.isEmpty()) tickPlacing();
        if (LIVE.isEmpty()) return;
        boolean check = e.getServer().getTickCount() % 5 == 0;
        Iterator<Map.Entry<ServerLevel, WordData>> levels = LIVE.entrySet().iterator();
        while (levels.hasNext()) {
            Map.Entry<ServerLevel, WordData> entry = levels.next();
            if (check) removeExpired(entry.getKey(), entry.getValue());
            if (entry.getValue().words.isEmpty()) levels.remove();
        }
    }

    private static void tickFlying(TickEvent.ServerTickEvent e) {
        Iterator<Flying> it = FLYING.iterator();
        while (it.hasNext()) {
            Flying f = it.next();
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(f.owner);
            ServerLevel world = (ServerLevel) f.text.level();
            Vec3 next = f.at.add(f.velocity);
            HitResult block = world.clip(new ClipContext(f.at, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, f.text));
            boolean stop = block.getType() != HitResult.Type.MISS || ++f.ticks >= f.maxTicks || p == null;
            // stop a hair before the wall, not exactly on it, or the letters would land inside it
            if (block.getType() != HitResult.Type.MISS) next = block.getLocation().subtract(f.velocity.normalize().scale(0.1));
            if (p != null) hitThings(p, f, world, next);
            f.at = next;
            f.text.setPos(next.x, next.y, next.z);
            if (!stop) continue;
            it.remove();
            f.text.discard();
            if (p != null) land(p, f, world, block.getType() != HitResult.Type.MISS);
        }
    }

    private static void hitThings(ServerPlayer p, Flying f, ServerLevel world, Vec3 to) {
        double half = 1 + WordRules.width(f.word) / 10.0;
        AABB box = new AABB(f.at, to).inflate(half);
        for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive())) {
            if (!f.hit.add(e.getId())) continue;
            e.hurt(world.damageSources().playerAttack(p), WordRules.damage(WordRules.letters(f.word), f.level));
            Fx.push(e, f.at, 1.2);
            if (VocalExpression.POWER.awakened(p)) doWhatItSays(p, f.word, world, e, e.position());
        }
    }

    private static void land(ServerPlayer p, Flying f, ServerLevel world, boolean crashed) {
        if (crashed && VocalExpression.POWER.awakened(p)) doWhatItSays(p, f.word, world, null, f.at);
        WordData data = data(world);
        WordData.Word word = new WordData.Word(p.getUUID(), world.getGameTime() + WordRules.lifeTicks(f.level));
        data.words.add(word);
        data.setDirty();
        LIVE.put(world, data);
        Deque<Long> spots = new ArrayDeque<>();
        Deque<Boolean> inks = new ArrayDeque<>();
        int width = WordRules.width(f.word);
        if (f.flat) {
            // lying flat ahead of you, solid all over so you can walk on it. The word runs away from you.
            BlockPos start = f.feet.below().relative(f.ahead);
            Direction side = f.ahead.getCounterClockWise();
            for (int c = 0; c < width; c++) {
                for (int r = 0; r < WordRules.ROWS; r++) {
                    spots.add(start.relative(f.ahead, c).relative(side, WordRules.ROWS / 2 - r).asLong());
                    inks.add(WordRules.ink(f.word, c, r));
                }
            }
        } else {
            // standing up facing you, letters only
            BlockPos middle = BlockPos.containing(f.at);
            Direction right = f.ahead.getClockWise();
            for (int c = 0; c < width; c++) {
                for (int r = 0; r < WordRules.ROWS; r++) {
                    if (!WordRules.ink(f.word, c, r)) continue;
                    spots.add(middle.relative(right, c - width / 2).above(WordRules.ROWS / 2 - r).asLong());
                    inks.add(true);
                }
            }
        }
        PLACING.add(new Placing(world, word, spots, inks));
    }

    private static void tickPlacing() {
        Iterator<Placing> it = PLACING.iterator();
        int budget = PLACE_PER_TICK;
        while (it.hasNext() && budget > 0) {
            Placing job = it.next();
            while (budget > 0 && !job.spots().isEmpty()) {
                budget--;
                BlockPos pos = BlockPos.of(job.spots().poll());
                boolean ink = job.inks().poll();
                if (!job.level().isLoaded(pos) || !job.level().getBlockState(pos).isAir()) continue;
                job.level().setBlock(pos, WORD.get().defaultBlockState().setValue(WordBlock.INK, ink), Block.UPDATE_CLIENTS);
                job.word().spots.add(pos.asLong());
            }
            if (job.spots().isEmpty()) {
                it.remove();
                if (!job.word().spots.isEmpty()) {
                    BlockPos at = BlockPos.of(job.word().spots.iterator().next());
                    job.level().playSound(null, at, PowerSounds.WORD_THUNK.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 0.9f + job.level().random.nextFloat() * 0.2f);
                }
            }
        }
    }

    private static void removeExpired(ServerLevel world, WordData data) {
        long now = world.getGameTime();
        int budget = REMOVE_PER_TICK * 5;
        Iterator<WordData.Word> it = data.words.iterator();
        while (it.hasNext() && budget > 0) {
            WordData.Word word = it.next();
            if (word.expires > now) continue;
            Iterator<Long> spots = word.spots.iterator();
            while (spots.hasNext() && budget > 0) {
                BlockPos pos = BlockPos.of(spots.next());
                if (!world.isLoaded(pos)) continue;
                budget--;
                if (world.getBlockState(pos).is(WORD.get())) world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                spots.remove();
            }
            if (word.spots.isEmpty() && notBeingPlaced(word)) it.remove();
            data.setDirty();
        }
    }

    private static boolean notBeingPlaced(WordData.Word word) {
        for (Placing job : PLACING) {
            if (job.word() == word) return false;
        }
        return true;
    }

    // Awakened: words do what they say when they hit (see TrueWords).
    private static void doWhatItSays(ServerPlayer p, String word, ServerLevel world, LivingEntity target, Vec3 at) {
        TrueWords.run(p, word, world, target, at);
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        for (Flying f : FLYING) f.text.discard();
        FLYING.clear();
        PLACING.clear();
        LIVE.clear();
    }

    static void noRoom(ServerPlayer p) {
        OwnPower.whisper(p, "Not enough breath for that word.");
    }
}
