package com.hexzeug.forceitemchallenge;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
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
    private final ServerBossBar displayBossBar;


    private Challenge(ServerPlayerEntity player) {
        this.player = player;
        this.server = Objects.requireNonNull(player.getServer());
        this.forceItemState = ForceItemState.getState(server);
        this.playerData = forceItemState.getPlayerData(player);
        this.random = server.getOverworld().getOrCreateRandom(Identifier.of(
                ForceItemChallenge.MOD_NAMESPACE,
                "challenge/" + player.getUuidAsString()
        ));
        this.displayBossBar = new ServerBossBar(
                playerData.getChallenge().getName(),
                BossBar.Color.WHITE,
                BossBar.Style.PROGRESS
        );
        displayBossBar.setVisible(!playerData.getChallenge().isEmpty());
        displayBossBar.addPlayer(player);
    }

    public static Challenge ofPlayer(ServerPlayerEntity player) {
        return challengeMap.computeIfAbsent(player, Challenge::new);
    }

    public void nextChallenge(boolean markComplete) {
        if (markComplete) {
            server.getPlayerManager().broadcast(
                    Text.empty()
                            .append(player.getName())
                            .append(Text.literal(" completed "))
                            .append(playerData.getChallenge().toHoverableText()),
                    false
            );
            playerData.markComplete();
            player.playSoundToPlayer(
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    SoundCategory.MASTER,
                    100,
                    1.0f
            );
        }

        // TODO: real challenge generation
        int max = Registries.ITEM.size();
        int next = random.nextInt(max);
        ItemStack challenge = new ItemStack(Registries.ITEM.get(next));
        this.setChallenge(challenge);
    }

    public void setChallenge(ItemStack challenge) {
        if (challenge == null) challenge = ItemStack.EMPTY;
        playerData.setChallenge(challenge);
        displayBossBar.setVisible(!challenge.isEmpty());
        displayBossBar.setName(challenge.getName());
    }

    public boolean isChallenge(ItemStack challenge) {
        return ItemStack.areItemsAndComponentsEqual(playerData.getChallenge(), challenge);
    }

    public ItemStack getChallenge() {
        return playerData.getChallenge();
    }
}
