package com.hexzeug.forceitemchallenge;

import com.hexzeug.forceitemchallenge.persistence.ForceItemChallengeState;
import com.hexzeug.forceitemchallenge.persistence.HistoryEntry;
import com.hexzeug.forceitemchallenge.persistence.PlayerState;
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
    private final ForceItemChallengeState forceItemChallengeState;
    private final PlayerState playerState;
    private final Random random;
    private final ServerBossBar displayBossBar;


    private Challenge(ServerPlayerEntity player) {
        this.player = player;
        this.server = Objects.requireNonNull(player.getServer());
        this.forceItemChallengeState = ForceItemChallengeState.ofServer(server);
        this.playerState = forceItemChallengeState.getOrCreatePlayerState(player.getUuid());
        this.random = server.getOverworld().getOrCreateRandom(Identifier.of(
                ForceItemChallenge.MOD_NAMESPACE,
                "challenge/" + player.getUuidAsString()
        ));
        this.displayBossBar = new ServerBossBar(
                playerState.getChallenge().orElse(ItemStack.EMPTY).getName(),
                BossBar.Color.WHITE,
                BossBar.Style.PROGRESS
        );
        displayBossBar.setVisible(playerState.getChallenge().isPresent());
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
                            .append(playerState.getChallenge().orElse(ItemStack.EMPTY).toHoverableText()),
                    false
            );
            player.playSoundToPlayer(
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    SoundCategory.MASTER,
                    100,
                    1.0f
            );
        }

        if (playerState.getChallenge().isPresent()) {
            playerState.addHistoryEntry(new HistoryEntry(playerState.getChallenge().get(), markComplete ? HistoryEntry.Type.COMPLETED : HistoryEntry.Type.SKIPPED));
        }

        // TODO: real challenge generation
        int max = Registries.ITEM.size();
        int next = random.nextInt(max);
        ItemStack challenge = new ItemStack(Registries.ITEM.get(next));
        this.setChallenge(challenge);
    }

    public void setChallenge(ItemStack challenge) {
        if (challenge == null) challenge = ItemStack.EMPTY;
        playerState.setChallenge(challenge);
        displayBossBar.setVisible(!challenge.isEmpty());
        displayBossBar.setName(challenge.getName());
    }

    public boolean isChallenge(ItemStack challenge) {
        return ItemStack.areItemsAndComponentsEqual(playerState.getChallenge().orElse(ItemStack.EMPTY), challenge);
    }

    public ItemStack getChallenge() {
        return playerState.getChallenge().orElse(ItemStack.EMPTY);
    }
}
