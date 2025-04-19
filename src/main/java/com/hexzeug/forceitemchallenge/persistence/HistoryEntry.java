package com.hexzeug.forceitemchallenge.persistence;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;

public record HistoryEntry(ItemStack item, Type type) {
    private static final String ITEM_TAG = "item";
    private static final String TYPE_TAG = "type";

    public static HistoryEntry fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        ItemStack item = ItemStack.fromNbt(registries, nbt.getCompound(ITEM_TAG)).orElseThrow();
        Type type = Type.valueOf(nbt.getString(TYPE_TAG));
        return new HistoryEntry(item, type);
    }

    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        nbt.put(ITEM_TAG, item().toNbt(registries));
        nbt.putString(TYPE_TAG, type().name());
        return nbt;
    }

    public enum Type {
        COMPLETED,
        SKIPPED,
    }
}
