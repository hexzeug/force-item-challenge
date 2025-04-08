package com.hexzeug.forceitemchallenge;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;
import org.apache.commons.compress.utils.Lists;

import java.util.*;

public class ForceItemState extends PersistentState {
    private static final String STATE_ID = "force_item";
    private static final String DURATION_TAG = "duration";
    private static final String RUNNING_TAG = "running";
    private static final String PLAYERS_TAG = "players";
    public long duration;
    public boolean running;
    private final Map<UUID, PlayerData> players;

    public ForceItemState() {
        this.duration = 72000; // 1 hour in ticks
        this.running = false;
        this.players = new HashMap<>();
    }

    public static ForceItemState getState(MinecraftServer server) {
        PersistentState.Type<ForceItemState> type = new Type<>(
                ForceItemState::new,
                ForceItemState::stateFromNbt,
                null
        );
        ForceItemState state = server
                .getOverworld()
                .getPersistentStateManager()
                .getOrCreate(type, STATE_ID);
        state.markDirty();
        return state;
    }

    public static ForceItemState stateFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        ForceItemState state = new ForceItemState();
        state.duration = nbt.getLong(DURATION_TAG);
        state.running = nbt.getBoolean(RUNNING_TAG);
        NbtCompound playersNbt = nbt.getCompound(PLAYERS_TAG);
        playersNbt.getKeys().forEach((uuidString) -> {
            UUID uuid = UUID.fromString(uuidString);
            NbtCompound playerNbt = playersNbt.getCompound(uuidString);
            state.players.put(uuid, PlayerData.fromNbt(playerNbt, registries));
        });
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        nbt.putLong(DURATION_TAG, duration);
        nbt.putBoolean(RUNNING_TAG, running);
        NbtCompound playersNbt = new NbtCompound();
        this.players.forEach((uuid, playerData) ->
                playersNbt.put(uuid.toString(), playerData.toNbt(registries))
        );
        nbt.put(PLAYERS_TAG, playersNbt);
        return nbt;
    }

    public PlayerData getPlayerData(ServerPlayerEntity player) {
        return this.players.computeIfAbsent(player.getUuid(), (uuid) -> new PlayerData());
    }

    public static class PlayerData {
        private static final String CHALLENGE_TAG = "challenge";
        private static final String COMPLETED_TAG = "completed";

        private ItemStack challenge;
        private final List<ItemStack> completedChallenges;

        public PlayerData() {
            this.challenge = ItemStack.EMPTY;
            this.completedChallenges = Lists.newArrayList();
        }

        public static PlayerData fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
            PlayerData playerData = new PlayerData();
            if (nbt.contains(CHALLENGE_TAG)) {
                playerData.challenge = ItemStack.fromNbtOrEmpty(registries, nbt.getCompound(CHALLENGE_TAG));
            }
            NbtList completedNbt = nbt.getList(COMPLETED_TAG, NbtElement.COMPOUND_TYPE);
            playerData.completedChallenges.addAll(
                    completedNbt
                        .stream()
                        .map((itemNbt) -> ItemStack.fromNbt(registries, itemNbt))
                        .flatMap(Optional::stream)
                        .toList()
            );
            return playerData;
        }

        public NbtCompound toNbt(RegistryWrapper.WrapperLookup registries) {
            NbtCompound nbt = new NbtCompound();
            if (!challenge.isEmpty()) {
                nbt.put(CHALLENGE_TAG, challenge.toNbt(registries));
            }
            NbtList completedNbt = new NbtList();
            completedChallenges.forEach(itemStack -> completedNbt.add(itemStack.toNbt(registries)));
            nbt.put(COMPLETED_TAG, completedNbt);
            return nbt;
        }

        public ItemStack getChallenge() {
            return challenge;
        }

        public void markComplete() {
            markComplete(challenge);
        }

        public void markComplete(ItemStack challenge) {
            if (challenge.isEmpty()) return;
            completedChallenges.add(challenge);
        }

        public void setChallenge(ItemStack challenge) {
            this.challenge = challenge;
        }
    }
}
