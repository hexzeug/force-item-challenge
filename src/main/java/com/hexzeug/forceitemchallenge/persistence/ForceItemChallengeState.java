package com.hexzeug.forceitemchallenge.persistence;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ForceItemChallengeState extends PersistentState {
    private static final PersistentState.Type<ForceItemChallengeState> STATE_TYPE =
            new Type<>(
                    ForceItemChallengeState::new,
                    ForceItemChallengeState::fromNbt,
                    null
            );
    private static final String STATE_ID = "force_item_challenge";

    private static final String RUNNING_TAG = "running";
    private boolean running;
    private static final String DURATION_TAG = "duration";
    private long duration;
    private static final String PLAYERS_TAG = "players";
    private final Map<UUID, PlayerState> players;

    private ForceItemChallengeState() {
        running = false;
        duration = 72000; // 1 hour in ticks
        players = HashMap.newHashMap(16);
    }

    public static ForceItemChallengeState ofServer(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(STATE_TYPE, STATE_ID);
    }

    private static ForceItemChallengeState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        ForceItemChallengeState state = new ForceItemChallengeState();
        state.running = nbt.getBoolean(RUNNING_TAG);
        state.duration = nbt.getLong(DURATION_TAG);

        NbtCompound playersNbt = nbt.getCompound(PLAYERS_TAG);
        for (String uuidString : playersNbt.getKeys()) {
            state.players.put(
                    UUID.fromString(uuidString),
                    PlayerState.fromNbt(playersNbt.getCompound(uuidString), registries)
            );
        }

        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        nbt.putBoolean(RUNNING_TAG, running);
        nbt.putLong(DURATION_TAG, duration);

        NbtCompound playersNbt = new NbtCompound();
        for (UUID uuid : players.keySet()) {
            playersNbt.put(
                    uuid.toString(),
                    players.get(uuid).writeNbt(new NbtCompound(), registries)
            );
        }
        nbt.put(PLAYERS_TAG, playersNbt);

        return nbt;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
        markDirty();
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
        markDirty();
    }

    public PlayerState getOrCreatePlayerState(UUID uuid) {
        if (!players.containsKey(uuid)) {
            players.put(uuid, new PlayerState());
            markDirty();
        }
        return players.get(uuid);
    }

    @Override
    public boolean isDirty() {
        return super.isDirty() || players.values().stream().anyMatch(PlayerState::isDirty);
    }
}
