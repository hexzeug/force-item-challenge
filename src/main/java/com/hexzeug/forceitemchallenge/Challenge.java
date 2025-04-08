package com.hexzeug.forceitemchallenge;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

public class Challenge {
    private static final Map<ServerPlayerEntity, Challenge> challengeMap = new WeakHashMap<>();
    private final ServerPlayerEntity player;
    private final MinecraftServer server;
    private final ForceItemState forceItemState;
    private final ForceItemState.PlayerData playerData;
    private final Random random;


    private Challenge(ServerPlayerEntity player) {
        this.player = player;
        this.server = Objects.requireNonNull(player.getServer());
        this.forceItemState = ForceItemState.getState(server);
        this.playerData = forceItemState.getPlayerData(player);
        this.random = server.getOverworld().getOrCreateRandom(Identifier.of(
                ForceItemChallenge.MOD_NAMESPACE,
                "challenge/" + player.getNameForScoreboard()
        ));
    }

    public static Challenge ofPlayer(ServerPlayerEntity player) {
        return challengeMap.computeIfAbsent(player, Challenge::new);
    }

    public void nextChallenge(boolean markComplete) {
        if (markComplete) playerData.markComplete();

        // TODO: real challenge generation
        int max = Registries.ITEM.size();
        int next = random.nextInt(max);
        ItemStack challenge = new ItemStack(Registries.ITEM.get(next));
        playerData.setChallenge(challenge);

        // TODO: display better
        player.sendMessage(challenge.getName());
    }

    public boolean isChallenge(ItemStack challenge) {
        return ItemStack.areItemsAndComponentsEqual(playerData.getChallenge(), challenge);
    }
}
