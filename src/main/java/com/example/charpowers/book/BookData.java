package com.example.charpowers.book;

import com.example.charpowers.book.model.BookRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// One owner's book: its pages, its deaths and the journal of what they've done since. In its own file, so a long journal
// costs nothing to anyone else, and nothing at all until the first page is written.
public final class BookData extends SavedData {
    final CompoundTag[] pages = new CompoundTag[BookRules.MAX_PAGES];
    final List<CompoundTag> journal = new ArrayList<>();
    final List<CompoundTag> deaths = new ArrayList<>();
    // what's been noted since the last page: only the first change at a spot matters when going back
    final Set<String> seen = new HashSet<>();
    long start, timeline;
    int kills;
    boolean warnedFull;

    static BookData of(MinecraftServer server, UUID owner) {
        return server.overworld().getDataStorage().computeIfAbsent(BookData::load, BookData::new, "charpowers_book_" + owner);
    }

    private static BookData load(CompoundTag tag) {
        BookData d = new BookData();
        ListTag pages = tag.getList("pages", Tag.TAG_COMPOUND);
        for (int i = 0; i < pages.size() && i < d.pages.length; i++) {
            CompoundTag page = pages.getCompound(i);
            d.pages[i] = page.isEmpty() ? null : page;
        }
        tag.getList("journal", Tag.TAG_COMPOUND).forEach(t -> d.journal.add((CompoundTag) t));
        tag.getList("deaths", Tag.TAG_COMPOUND).forEach(t -> d.deaths.add((CompoundTag) t));
        d.start = tag.getLong("start");
        d.timeline = tag.getLong("timeline");
        d.kills = tag.getInt("kills");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (CompoundTag page : pages) list.add(page == null ? new CompoundTag() : page);
        tag.put("pages", list);
        ListTag entries = new ListTag();
        entries.addAll(journal);
        tag.put("journal", entries);
        ListTag held = new ListTag();
        held.addAll(deaths);
        tag.put("deaths", held);
        tag.putLong("start", start);
        tag.putLong("timeline", timeline);
        tag.putInt("kills", kills);
        return tag;
    }

    boolean anyPage() {
        for (CompoundTag page : pages) {
            if (page != null) return true;
        }
        return false;
    }

    long end() {
        return start + journal.size();
    }

    private long[] indexes() {
        long[] out = new long[pages.length];
        for (int i = 0; i < pages.length; i++) out[i] = pages[i] == null ? -1 : pages[i].getLong("index");
        return out;
    }

    // Forgets anything no page could go back past.
    void trim() {
        drop((int) (BookRules.oldestNeeded(indexes(), end()) - start));
    }

    void drop(int count) {
        if (count <= 0) return;
        journal.subList(0, count).clear();
        start += count;
        kills = 0;
        for (CompoundTag entry : journal) {
            if (entry.contains("id")) kills++;
        }
    }
}
