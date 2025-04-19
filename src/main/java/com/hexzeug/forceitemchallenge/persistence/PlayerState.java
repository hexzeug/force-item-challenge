package com.hexzeug.forceitemchallenge.persistence;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PlayerState extends PersistentState {
    private static final String HISTORY_TAG = "history";
    private final List<HistoryEntry> history;
    private static final String CHALLENGE_TAG = "challenge";
    private ItemStack challenge;

    protected PlayerState() {
        history = new ArrayList<>();
    }

    public static PlayerState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        PlayerState state = new PlayerState();

        NbtList historyNbt = nbt.getList(HISTORY_TAG, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < historyNbt.size(); i++) {
            state.history.add(HistoryEntry.fromNbt(historyNbt.getCompound(i), registries));
        }

        state.challenge = ItemStack.fromNbt(registries, nbt.getCompound(CHALLENGE_TAG)).orElse(null);

        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList historyNbt = new NbtList();
        for (HistoryEntry historyEntry : history) {
            historyNbt.add(historyEntry.writeNbt(new NbtCompound(), registries));
        }
        nbt.put(HISTORY_TAG, historyNbt);

        if (challenge != null) {
            nbt.put(CHALLENGE_TAG, challenge.toNbt(registries));
        }

        return nbt;
    }

    public List<HistoryEntry> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public void addHistoryEntry(HistoryEntry historyEntry) {
        history.add(historyEntry);
        markDirty();
    }

    public Optional<ItemStack> getChallenge() {
        return Optional.ofNullable(challenge);
    }

    public void setChallenge(ItemStack challenge) {
        this.challenge = challenge;
        markDirty();
    }
}
