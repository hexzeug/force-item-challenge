package com.hexzeug.forceitemchallenge;

import com.hexzeug.forceitemchallenge.persistence.ForceItemChallengeState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ChallengeMaster {
    private static ChallengeMaster instance;
    private final MinecraftServer server;
    private final ForceItemChallengeState state;

    private ChallengeMaster(MinecraftServer server) {
        this.server = server;
        this.state = ForceItemChallengeState.ofServer(server);
    }

    public static ChallengeMaster ofServer(MinecraftServer server) {
        if (instance == null || instance.server != server) {
            instance = new ChallengeMaster(server);
        }
        return instance;
    }

    public void tick() {
        server.getPlayerManager().getPlayerList().forEach(this::tickPlayer);

        if (server.getTickManager().isFrozen() == state.isRunning()) {
            server.getTickManager().setFrozen(!state.isRunning());
        }
        Freezer.ofServer(server).tick();
        if (state.isRunning()
                ? server.getOverworld().getTime() % 20 == 0
                : server.getTicks() % 20 == 0
        ) {
            sendTimer();
        }
    }

    private void tickPlayer(ServerPlayerEntity player) {
        Challenge challenge = Challenge.ofPlayer(player);
        if (state.isRunning()
                && server.getOverworld().getTime() < state.getDuration()
                && player.getInventory().contains(challenge::isChallenge)
        ) {
            challenge.nextChallenge(true);
        }

        if (server.getOverworld().getTime() == state.getDuration()) {
            player.playSoundToPlayer(
                    SoundEvents.ENTITY_WITHER_SPAWN,
                    SoundCategory.MASTER,
                    100,
                    1.0f
            );
        }
    }

    private void sendTimer() {
        Text timerText = new Timer(state.getDuration() - server.getOverworld().getTime())
                .toFormattedText(Formatting.GOLD, Formatting.RED, Formatting.BOLD);
        server.getPlayerManager().getPlayerList().forEach(player ->
                player.sendMessageToClient(timerText, true)
        );
    }

    public boolean start() {
        if (state.isRunning()) return false;
        state.setRunning(true);
        return true;
    }

    public boolean pause() {
        if (!state.isRunning()) return false;
        state.setRunning(false);
        return true;
    }

    public boolean setDuration(long duration) {
        if (duration < server.getOverworld().getTime()) return false;
        state.setDuration(duration);
        return true;
    }

    public long getDuration() {
        return state.getDuration();
    }

    public static class Timer {
        private final int seconds;
        private final int minutes;
        private final int hours;

        public Timer(long time) {
            long nonNegativeTime = Math.max(0, time);
            long secondsLeft = nonNegativeTime / 20;
            long minutesLeft = secondsLeft / 60;
            long hoursLeft = minutesLeft / 60;
            this.seconds = (int) (secondsLeft % 60);
            this.minutes = (int) (minutesLeft % 60);
            this.hours = (int) hoursLeft;
        }

        public Text toText() {
            return toFormattedText(Formatting.RESET).copy().fillStyle(Style.EMPTY);
        }

        public Text toFormattedText(Formatting number, Formatting... global) {
            MutableText start;
            if (hours > 0) {
                start = Text.empty()
                        .append(Text
                                .literal(Integer.toString(hours))
                                .formatted(number)
                        )
                        .append(":");
            } else {
                start = Text.empty();
            }
            String paddedSeconds = (seconds < 10) ? "0" + seconds : Integer.toString(seconds);
            String paddedMinutes = (minutes < 10) ? "0" + minutes : Integer.toString(minutes);
            return start
                    .append(Text
                            .literal(paddedMinutes)
                            .formatted(number)
                    )
                    .append(":")
                    .append(Text
                            .literal(paddedSeconds)
                            .formatted(number)
                    )
                    .formatted(global);
        }
    }
}
