package com.hexzeug.forceitemchallenge;

import com.hexzeug.forceitemchallenge.display.ChallengeDisplay;
import com.hexzeug.forceitemchallenge.persistence.ForceItemChallengeState;
import com.hexzeug.forceitemchallenge.persistence.HistoryEntry;
import com.hexzeug.forceitemchallenge.persistence.PlayerState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.util.*;
import java.util.stream.Collectors;

public class Challenger {
    private static final Map<MinecraftServer, Challenger> instances = WeakHashMap.newWeakHashMap(1);

    private final MinecraftServer server;
    private final ForceItemChallengeState state;
    private final Map<ServerPlayerEntity, ChallengeDisplay> displays;

    private Challenger(MinecraftServer server) {
        this.server = server;
        this.state = ForceItemChallengeState.ofServer(server);
        this.displays = WeakHashMap.newWeakHashMap(1);
    }

    public static Challenger ofServer(MinecraftServer server) {
        return instances.computeIfAbsent(server, Challenger::new);
    }

    public void tick() {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ItemStack challenge = getOrCreateChallenge(player);
            if (player.getInventory().contains(itemStack -> compareWithChallenge(itemStack, challenge))) {
                PlayerState playerState = playerState(player);
                playerState.addHistoryEntry(new HistoryEntry(challenge, HistoryEntry.Type.COMPLETED));
                playerState.setChallenge(null);
            }
        }
    }

    public void skipChallengeOf(ServerPlayerEntity player) {
        PlayerState playerState = playerState(player);
        getChallenge(player).ifPresent(challenge ->
                playerState.addHistoryEntry(new HistoryEntry(challenge, HistoryEntry.Type.SKIPPED))
        );
        playerState.setChallenge(null);
    }

    public void silentlySkipChallengeOf(ServerPlayerEntity player) {
        playerState(player).setChallenge(null);
    }

    private PlayerState playerState(ServerPlayerEntity player) {
        return state.getOrCreatePlayerState(player.getUuid());
    }

    public ChallengeDisplay challengeDisplay(ServerPlayerEntity player) {
        return displays.computeIfAbsent(player, ChallengeDisplay::new);
    }

    private Random random(ServerPlayerEntity player) {
        return server.getOverworld().getOrCreateRandom(Identifier.of(
                ForceItemChallenge.MOD_NAMESPACE,
                "challenge/" + player.getNameForScoreboard().toLowerCase()
        ));
    }

    public Optional<ItemStack> getChallenge(ServerPlayerEntity player) {
        // if every player has personal challenges
        return playerState(player).getChallenge();
    }

    public ItemStack getOrCreateChallenge(ServerPlayerEntity player) {
        // if every player has personal challenges
        PlayerState playerState = playerState(player);
        return playerState.getChallenge().orElseGet(() -> {
            ItemStack challenge = generateRandomChallenge(
                    random(player),
                    playerState.getHistory().stream().map(HistoryEntry::item).collect(Collectors.toUnmodifiableSet())
            );
            playerState.setChallenge(challenge);
            challengeDisplay(player).updateChallenge(challenge);
            return challenge;
        });
    }

    private ItemStack generateRandomChallenge(Random random, Collection<ItemStack> exclude) {
        List<ItemStack> universe = Registries
                .ITEM
                .stream()
                .map(ItemStack::new)
                .filter(universeStack -> exclude
                        .stream()
                        .noneMatch(excludeStack -> compareWithChallenge(excludeStack, universeStack))
                )
                .toList();
        int i = random.nextInt(universe.size());
        return universe.get(i);
    }

    private boolean compareWithChallenge(ItemStack itemStack, ItemStack challenge) {
        return ItemStack.areItemsEqual(itemStack, challenge);
    }
}
