package com.example.charpowers.book;

import com.example.charpowers.own.Setting;
import com.example.charpowers.CharPowers;
import com.example.charpowers.Net;
import com.example.charpowers.book.model.BookRules;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.own.OwnPower;
import com.example.charpowers.sound.PowerSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

// Keep moments in a book and bring time back to them. Everything YOU did since the page is noted down in a journal (only once
// a page exists), and restoring plays it backwards a bit at a time. Its awakening isn't decided yet: that's Char's to pick.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class BookKeeping extends OwnPower {
    public static final BookKeeping POWER = new BookKeeping();
    private static final String MARK = "charpowers_mark";
    private static final List<Undo> UNDOS = new ArrayList<>();
    private static boolean anyDead;
    private static BlockPos pendingRoom;

    private static final class Undo {
        final UUID owner;
        final List<CompoundTag> entries;
        int at;

        Undo(UUID owner, List<CompoundTag> entries) {
            this.owner = owner;
            this.entries = entries;
        }
    }

    private BookKeeping() {
        super(Mastery.BOOK_KEEPING, Items.WRITABLE_BOOK);
    }

    @Override
    public String unlockMessage() { return "The book falls open by itself. Whatever you do from now on, it can take back."; }

    @Override
    public String awakenedMessage() { return "The book is whole again."; }

    @Override
    public String opInfo(MinecraftServer server) {
        return BookWorld.of(server.overworld()).where();
    }

    private static BookData book(ServerPlayer p) {
        return BookData.of(p.getServer(), p.getUUID());
    }

    @Override
    public void everySecond(ServerPlayer p) {
        BookData d = book(p);
        if (d.anyPage()) Mastery.add(p, id, 1);
        if (d.deaths.isEmpty()) return;
        // walk back near where you died and it's all yours again
        double range = BookRules.deathRange(mastery(p));
        String here = p.level().dimension().location().toString();
        Iterator<CompoundTag> it = d.deaths.iterator();
        while (it.hasNext()) {
            CompoundTag death = it.next();
            if (!here.equals(death.getString("dim")) || p.distanceToSqr(death.getDouble("x"), death.getDouble("y"), death.getDouble("z")) > range * range) continue;
            ListTag items = death.getList("items", Tag.TAG_COMPOUND);
            for (int i = 0; i < items.size(); i++) Fx.give(p, ItemStack.of(items.getCompound(i)));
            it.remove();
            d.setDirty();
            tell(p, "The book gives you back what you lost here.");
        }
    }

    @Override
    public void onKey(ServerPlayer p, String ability) {
        if ("open_book".equals(ability)) open(p);
    }

    @Override
    public void onClientAction(ServerPlayer p, String action) {
        if (!owns(p)) return;
        String[] parts = action.split(":");
        if (parts.length != 2) return;
        int page;
        try {
            page = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return;
        }
        if (page < 0 || page >= BookRules.pages(mastery(p))) return;
        if ("write".equals(parts[0])) write(p, page);
        else if ("restore".equals(parts[0])) restore(p, page);
        open(p);
    }

    public void open(ServerPlayer p) {
        Fx.sound(p, PowerSounds.PAGE_FLIP, 0.7f, 1f);
        BookData d = book(p);
        CompoundTag tag = new CompoundTag();
        tag.putInt("max", BookRules.pages(mastery(p)));
        tag.putBoolean("busy", rewinding(p));
        ListTag pages = new ListTag();
        for (int i = 0; i < d.pages.length; i++) {
            CompoundTag row = new CompoundTag();
            if (d.pages[i] != null) {
                CompoundTag page = d.pages[i];
                row.putString("where", (int) page.getDouble("x") + " " + (int) page.getDouble("y") + " " + (int) page.getDouble("z") + " in " + new ResourceLocation(page.getString("dim")).getPath());
                row.putInt("changes", (int) (d.end() - page.getLong("index")));
                row.putBoolean("short", page.getLong("index") < d.start);
            }
            pages.add(row);
        }
        tag.put("pages", pages);
        ListTag deaths = new ListTag();
        for (CompoundTag death : d.deaths) {
            CompoundTag row = new CompoundTag();
            row.putString("where", (int) death.getDouble("x") + " " + (int) death.getDouble("y") + " " + (int) death.getDouble("z") + " in " + new ResourceLocation(death.getString("dim")).getPath());
            row.putInt("items", death.getList("items", Tag.TAG_COMPOUND).size());
            deaths.add(row);
        }
        tag.put("deaths", deaths);
        tag.putDouble("range", BookRules.deathRange(mastery(p)));
        Net.toOwner(p, id.toString(), tag);
    }

    public boolean rewinding(ServerPlayer p) {
        for (Undo u : UNDOS) {
            if (u.owner.equals(p.getUUID())) return true;
        }
        return false;
    }

    public int deathsHeld(ServerPlayer p) {
        return book(p).deaths.size();
    }

    // ---- writing a page: you, just as you are now

    public void write(ServerPlayer p, int page) {
        if (rewinding(p)) {
            whisper(p, "The book's still turning back.");
            return;
        }
        BookData d = book(p);
        d.deaths.clear(); // that timeline's gone
        d.timeline++;
        CompoundTag t = new CompoundTag();
        t.putDouble("x", p.getX());
        t.putDouble("y", p.getY());
        t.putDouble("z", p.getZ());
        t.putFloat("yaw", p.getYRot());
        t.putFloat("pitch", p.getXRot());
        t.putString("dim", p.level().dimension().location().toString());
        t.putFloat("health", p.getHealth());
        t.putInt("food", p.getFoodData().getFoodLevel());
        t.putFloat("sat", p.getFoodData().getSaturationLevel());
        t.putInt("xpLevel", p.experienceLevel);
        t.putFloat("xpBar", p.experienceProgress);
        t.putInt("xpTotal", p.totalExperience);
        ListTag effects = new ListTag();
        for (MobEffectInstance e : p.getActiveEffects()) effects.add(e.save(new CompoundTag()));
        t.put("effects", effects);
        t.put("inv", p.getInventory().save(new ListTag()));
        t.putLong("index", d.end());
        t.putLong("timeline", d.timeline);
        d.pages[page] = t;
        d.seen.clear();
        d.trim();
        d.warnedFull = false;
        d.setDirty();
        p.level().playSound(null, p.blockPosition(), PowerSounds.INK_SCRATCH.get(), SoundSource.PLAYERS, 1f, 1f);
        tell(p, "Page " + (page + 1) + " is written.");
    }

    // ---- restoring: back to the page. Items from the "future" stop existing, and pages written after it never happened.

    public void restore(ServerPlayer p, int page) {
        BookData d = book(p);
        CompoundTag t = d.pages[page];
        if (t == null) {
            whisper(p, "That page is blank.");
            return;
        }
        if (rewinding(p)) {
            whisper(p, "The book's still turning back.");
            return;
        }
        MinecraftServer server = p.getServer();
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(t.getString("dim"))));
        if (level == null) {
            whisper(p, "That place doesn't exist any more.");
            return;
        }
        // what comes back: newest first
        int from = (int) Math.max(0, t.getLong("index") - d.start);
        List<CompoundTag> back = new ArrayList<>(d.journal.subList(from, d.journal.size()));
        java.util.Collections.reverse(back);
        d.drop(0);
        d.journal.subList(from, d.journal.size()).clear();
        d.kills = 0;
        for (CompoundTag entry : d.journal) {
            if (entry.contains("id")) d.kills++;
        }
        for (int i = 0; i < d.pages.length; i++) {
            if (i != page && d.pages[i] != null && d.pages[i].getLong("index") > t.getLong("index")) d.pages[i] = null;
        }
        d.deaths.clear();
        BookWorld world = BookWorld.of(server.overworld());
        for (long mark = t.getLong("timeline"); mark <= d.timeline; mark++) world.deadMarks.add(BookRules.mark(p.getUUID().toString(), mark));
        world.setDirty();
        anyDead = true;
        d.timeline++;
        d.seen.clear();
        d.setDirty();

        if (p.level() != level) p.teleportTo(level, t.getDouble("x"), t.getDouble("y"), t.getDouble("z"), t.getFloat("yaw"), t.getFloat("pitch"));
        else p.connection.teleport(t.getDouble("x"), t.getDouble("y"), t.getDouble("z"), t.getFloat("yaw"), t.getFloat("pitch"));
        p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        p.fallDistance = 0;
        p.setHealth(t.getFloat("health"));
        p.getFoodData().setFoodLevel(t.getInt("food"));
        p.getFoodData().setSaturation(t.getFloat("sat"));
        p.setExperienceLevels(t.getInt("xpLevel"));
        p.experienceProgress = t.getFloat("xpBar");
        p.totalExperience = t.getInt("xpTotal");
        p.removeAllEffects();
        ListTag effects = t.getList("effects", Tag.TAG_COMPOUND);
        for (int i = 0; i < effects.size(); i++) {
            MobEffectInstance e = MobEffectInstance.load(effects.getCompound(i));
            if (e != null) p.addEffect(e);
        }
        p.getInventory().load(t.getList("inv", Tag.TAG_COMPOUND));
        p.inventoryMenu.broadcastChanges();
        vanishEverywhere(server, world);

        level.playSound(null, p.blockPosition(), PowerSounds.PAGES_REWIND.get(), SoundSource.PLAYERS, 1f, 1f);
        Fx.particles(level, ParticleTypes.ENCHANT, p.position().add(0, 1, 0), 60, 0.8);
        if (t.getLong("index") < d.start) whisper(p, "This page is so old the book couldn't remember everything since.");
        tell(p, back.isEmpty() ? "Time goes back to page " + (page + 1) + "." : "Time goes back to page " + (page + 1) + ". " + back.size() + " things are being undone.");
        if (!back.isEmpty()) UNDOS.add(new Undo(p.getUUID(), back));
    }

    private static void undoOne(MinecraftServer server, CompoundTag entry) {
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(entry.getString("dim"))));
        if (level == null) return;
        if (entry.contains("id")) {
            CompoundTag nbt = entry.getCompound("n");
            nbt.remove("UUID");
            nbt.remove("DeathTime");
            nbt.remove("HurtTime");
            nbt.putString("id", entry.getString("id"));
            Entity mob = EntityType.create(nbt, level).orElse(null);
            if (mob == null) return;
            if (mob instanceof LivingEntity living) living.setHealth(living.getMaxHealth());
            level.addFreshEntity(mob);
            return;
        }
        BlockPos pos = BlockPos.of(entry.getLong("p"));
        BlockState state = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), entry.getCompound("s"));
        level.setBlock(pos, state, 2 | 16);
        if (entry.contains("e")) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                be.load(entry.getCompound("e"));
                be.setChanged();
                level.sendBlockUpdated(pos, state, state, 2);
            }
        }
    }

    private static int cost(MinecraftServer server, CompoundTag entry) {
        if (entry.contains("id")) return 1;
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(entry.getString("dim"))));
        BlockPos pos = BlockPos.of(entry.getLong("p"));
        return level == null || level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4) ? 1 : BookRules.UNLOADED_COST;
    }

    // ---- the journal

    private void note(ServerPlayer p, Level level, BlockPos pos, BlockState before, boolean withEntity) {
        BookData d = book(p);
        if (!d.anyPage()) return;
        String dim = level.dimension().location().toString();
        if (!d.seen.add(dim + "|" + pos.asLong())) return;
        CompoundTag entry = new CompoundTag();
        entry.putString("dim", dim);
        entry.putLong("p", pos.asLong());
        entry.put("s", NbtUtils.writeBlockState(before));
        if (withEntity) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) entry.put("e", be.saveWithFullMetadata());
        }
        addEntry(p, d, entry);
    }

    private void addEntry(ServerPlayer p, BookData d, CompoundTag entry) {
        d.journal.add(entry);
        int drop = BookRules.toDrop(d.journal.size());
        if (drop > 0) {
            d.drop(drop);
            if (!d.warnedFull) {
                d.warnedFull = true;
                whisper(p, "The book's nearly full: your oldest page won't be able to undo everything any more.");
            }
        }
        d.setDirty();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (com.example.charpowers.hidden.attack.Wreck.BREAKING) return; // only a power asking if it may, nothing broke
        if (!(e.getPlayer() instanceof ServerPlayer p) || !POWER.owns(p) || !(e.getLevel() instanceof Level level)) return;
        BlockState state = e.getState();
        POWER.note(p, level, e.getPos(), state, true);
        // the other half of a door or a bed goes with it
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            BlockPos other = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? e.getPos().above() : e.getPos().below();
            POWER.note(p, level, other, level.getBlockState(other), false);
        } else if (state.getBlock() instanceof BedBlock) {
            Direction way = state.getValue(BedBlock.FACING);
            BlockPos other = state.getValue(BedBlock.PART) == BedPart.FOOT ? e.getPos().relative(way) : e.getPos().relative(way.getOpposite());
            POWER.note(p, level, other, level.getBlockState(other), false);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (com.example.charpowers.hidden.attack.Wreck.BREAKING) return; // only a power asking if it may, nothing was placed
        if (!(e.getEntity() instanceof ServerPlayer p) || !POWER.owns(p) || !(e.getLevel() instanceof Level level)) return;
        if (e instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            for (BlockSnapshot snap : multi.getReplacedBlockSnapshots()) POWER.note(p, level, snap.getPos(), snap.getReplacedBlock(), false);
        } else {
            POWER.note(p, level, e.getPos(), e.getBlockSnapshot().getReplacedBlock(), false);
        }
    }

    // Before a chest is opened, note what's in it. It's cheaper than watching every move you make in it.
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !POWER.owns(p)) return;
        BlockEntity be = e.getLevel().getBlockEntity(e.getPos());
        if (be instanceof Container) POWER.note(p, e.getLevel(), e.getPos(), e.getLevel().getBlockState(e.getPos()), true);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof Player || !(e.getSource().getEntity() instanceof ServerPlayer p) || !POWER.owns(p)) return;
        BookData d = book(p);
        if (!d.anyPage() || d.kills >= Setting.BOOK_MAX_KILLS.i()) return;
        LivingEntity mob = e.getEntity();
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        CompoundTag entry = new CompoundTag();
        entry.putString("dim", mob.level().dimension().location().toString());
        entry.putString("id", type.toString());
        entry.put("n", mob.saveWithoutId(new CompoundTag()));
        d.kills++;
        POWER.addEntry(p, d, entry);
    }

    // Your things go into the book when you die, not onto the ground.
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !POWER.owns(p) || e.getDrops().isEmpty()) return;
        CompoundTag death = new CompoundTag();
        death.putString("dim", p.level().dimension().location().toString());
        death.putDouble("x", p.getX());
        death.putDouble("y", p.getY());
        death.putDouble("z", p.getZ());
        ListTag items = new ListTag();
        for (ItemEntity item : e.getDrops()) items.add(item.getItem().save(new CompoundTag()));
        death.put("items", items);
        BookData d = book(p);
        d.deaths.add(death);
        d.setDirty();
        e.setCanceled(true);
    }

    // ---- the no-dupes rule: what you throw away carries a mark, and vanishes if you go back past it

    @SubscribeEvent
    public static void onToss(ItemTossEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer p) || !POWER.owns(p)) return;
        BookData d = book(p);
        if (d.anyPage()) e.getEntity().getItem().getOrCreateTag().putString(MARK, BookRules.mark(p.getUUID().toString(), d.timeline));
    }

    // Made from something marked: it's marked too, so it can't be used to sneak the value out.
    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent e) {
        for (int i = 0; i < e.getInventory().getContainerSize(); i++) {
            ItemStack in = e.getInventory().getItem(i);
            if (in.hasTag() && in.getTag().contains(MARK)) {
                e.getCrafting().getOrCreateTag().putString(MARK, in.getTag().getString(MARK));
                return;
            }
        }
    }

    private static boolean dead(ItemStack stack, BookWorld world) {
        return stack.hasTag() && stack.getTag().contains(MARK) && world.deadMarks.contains(stack.getTag().getString(MARK));
    }

    private static void sweep(Container box, BookWorld world) {
        for (int i = 0; i < box.getContainerSize(); i++) {
            if (dead(box.getItem(i), world)) box.setItem(i, ItemStack.EMPTY);
        }
    }

    private static void sweepPlayer(Player p, BookWorld world) {
        sweep(p.getInventory(), world);
        sweep(p.getEnderChestInventory(), world);
    }

    private static void vanishEverywhere(MinecraftServer server, BookWorld world) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) sweepPlayer(p, world);
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity e : level.getAllEntities()) {
                if (e instanceof ItemEntity item && dead(item.getItem(), world)) item.discard();
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (anyDead && e.getEntity().getServer() != null) sweepPlayer(e.getEntity(), BookWorld.of(e.getEntity().getServer().overworld()));
    }

    @SubscribeEvent
    public static void onOpen(PlayerContainerEvent.Open e) {
        if (!anyDead || e.getEntity().getServer() == null) return;
        BookWorld world = BookWorld.of(e.getEntity().getServer().overworld());
        for (var slot : e.getContainer().slots) {
            if (dead(slot.getItem(), world)) slot.set(ItemStack.EMPTY);
        }
    }

    @SubscribeEvent
    public static void onItemJoin(EntityJoinLevelEvent e) {
        if (anyDead && e.getEntity() instanceof ItemEntity item && e.getLevel().getServer() != null && dead(item.getItem(), BookWorld.of(e.getLevel().getServer().overworld()))) e.setCanceled(true);
    }

    // ---- the one book in the world

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent e) {
        BookWorld world = BookWorld.of(e.getServer().overworld());
        world.choose(e.getServer().overworld());
        anyDead = !world.deadMarks.isEmpty();
    }

    @SubscribeEvent
    public static void onChunk(ChunkEvent.Load e) {
        if (!(e.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || !(e.getChunk() instanceof LevelChunk chunk)) return;
        if (pendingRoom == null && BookWorld.of(level).isTheChunk(chunk.getPos().x, chunk.getPos().z)) pendingRoom = chunk.getPos().getWorldPosition();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (pendingRoom != null) {
            pendingRoom = null;
            ServerLevel overworld = e.getServer().overworld();
            BookWorld.of(overworld).build(overworld);
        }
        if (UNDOS.isEmpty()) return;
        // a bit at a time, oldest request first, loading far-off chunks only slowly
        Undo u = UNDOS.get(0);
        int budget = BookRules.UNDO_PER_TICK;
        while (budget > 0 && u.at < u.entries.size()) {
            CompoundTag entry = u.entries.get(u.at++);
            budget -= cost(e.getServer(), entry);
            undoOne(e.getServer(), entry);
        }
        if (u.at >= u.entries.size()) {
            UNDOS.remove(0);
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(u.owner);
            if (p != null) tell(p, "Everything's back as it was.");
        }
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        // anything still being undone finishes now: a half-done rewind mustn't survive a restart
        for (Undo u : UNDOS) {
            while (u.at < u.entries.size()) undoOne(e.getServer(), u.entries.get(u.at++));
        }
        UNDOS.clear();
        pendingRoom = null;
        anyDead = false;
    }
}
