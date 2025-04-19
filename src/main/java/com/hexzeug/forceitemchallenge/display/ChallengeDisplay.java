package com.hexzeug.forceitemchallenge.display;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public class ChallengeDisplay {
    private final ServerPlayerEntity player;
    private final ServerBossBar bossBar;

    public ChallengeDisplay(ServerPlayerEntity player) {
        this.player = player;
        this.bossBar = new ServerBossBar(Text.empty(), BossBar.Color.YELLOW, BossBar.Style.PROGRESS);
        bossBar.setVisible(false);
        bossBar.addPlayer(player);
    }

    public void updateChallenge(ItemStack challenge) {
        if (challenge == null) {
            bossBar.setVisible(false);
            return;
        }
        bossBar.setName(challenge.getName());
        bossBar.setVisible(true);
        player.sendMessage(Text.literal("Your next challenge is ").append(challenge.toHoverableText()));
        player.playSoundToPlayer(
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.MASTER,
                100,
                1.0f
        );
    }
}
