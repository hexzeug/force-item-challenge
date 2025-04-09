package com.hexzeug.forceitemchallenge;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ChallengeMaster {
    private static ChallengeMaster instance;
    private final MinecraftServer server;
    private final ForceItemState state;

    private ChallengeMaster(MinecraftServer server) {
        this.server = server;
        this.state = ForceItemState.getState(server);
    }

    public static ChallengeMaster ofServer(MinecraftServer server) {
        if (instance == null || instance.server != server) {
            instance = new ChallengeMaster(server);
        }
        return instance;
    }

    public void tick() {
        long time = server.getOverworld().getTime();
        if (state.running) {
            server.getTickManager().setFrozen(false);
            if (time < state.duration) {
                if (time % 20 == 0) sendTimer();
                server.getPlayerManager().getPlayerList().forEach(player -> {
                    Challenge challenge = Challenge.ofPlayer(player);
                    if (player.getInventory().contains(challenge::isChallenge)) {
                        challenge.nextChallenge(true);
                    }
                });
            }
        } else {
            server.getTickManager().setFrozen(true);
            if (server.getTicks() % 20 == 0) sendTimer();
            //TODO: freeze players
        }
    }

    public boolean start() {
        if (state.running) return false;
        state.running = true;
        return true;
    }

    public boolean pause() {
        if (!state.running) return false;
        state.running = false;
        return true;
    }

    public boolean setDuration(long duration) {
        if (duration < server.getOverworld().getTime()) return false;
        state.duration = duration;
        return true;
    }

    public long getDuration() {
        return state.duration;
    }

    private void sendTimer() {
        Text timerText = new Timer(state.duration - server.getOverworld().getTime())
                .toFormattedText(Formatting.GOLD, Formatting.RED, Formatting.BOLD);
        server.getPlayerManager().getPlayerList().forEach(player ->
                player.sendMessageToClient(timerText, true)
        );
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
