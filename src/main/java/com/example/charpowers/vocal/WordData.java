package com.example.charpowers.vocal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Which word blocks are in this world and when they go. Saved with the world, so a server restart can't leave words
// standing for ever.
final class WordData extends SavedData {
    static final String NAME = "charpowers_words";

    static final class Word {
        final UUID owner;
        long expires;
        final List<Long> spots = new ArrayList<>();

        Word(UUID owner, long expires) {
            this.owner = owner;
            this.expires = expires;
        }
    }

    final List<Word> words = new ArrayList<>();

    static WordData load(CompoundTag tag) {
        WordData data = new WordData();
        for (Tag t : tag.getList("words", Tag.TAG_COMPOUND)) {
            CompoundTag w = (CompoundTag) t;
            Word word = new Word(w.getUUID("owner"), w.getLong("expires"));
            for (long spot : w.getLongArray("spots")) word.spots.add(spot);
            data.words.add(word);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Word word : words) {
            CompoundTag w = new CompoundTag();
            w.putUUID("owner", word.owner);
            w.putLong("expires", word.expires);
            w.putLongArray("spots", word.spots);
            list.add(w);
        }
        tag.put("words", list);
        return tag;
    }
}
