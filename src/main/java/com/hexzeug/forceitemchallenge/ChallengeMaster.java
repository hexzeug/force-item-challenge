package com.hexzeug.forceitemchallenge;

import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerPosition;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.*;

public class ChallengeMaster {
    private static ChallengeMaster instance;
    private final MinecraftServer server;
    private final ForceItemState state;
    private final Map<UUID, FreezeState> freezeStates;
    public static final Identifier FREEZE_MODIFIER_ID =
            Identifier.of(ForceItemChallenge.MOD_NAMESPACE, "freeze");

    private ChallengeMaster(MinecraftServer server) {
        this.server = server;
        this.state = ForceItemState.getState(server);
        this.freezeStates = new HashMap<>();
    }

    public static ChallengeMaster ofServer(MinecraftServer server) {
        if (instance == null || instance.server != server) {
            instance = new ChallengeMaster(server);
        }
        return instance;
    }

    public void tick() {
        server.getPlayerManager().getPlayerList().forEach(this::tickPlayer);

        if (server.getTickManager().isFrozen() == state.running) {
            server.getTickManager().setFrozen(!state.running);
        }
        if (state.running
                ? server.getOverworld().getTime() % 20 == 0
                : server.getTicks() % 20 == 0
        ) {
            sendTimer();
        }
    }

    private void tickPlayer(ServerPlayerEntity player) {
        tickPlayerFreezing(player);

        Challenge challenge = Challenge.ofPlayer(player);
        if (state.running
                && server.getOverworld().getTime() < state.duration
                && player.getInventory().contains(challenge::isChallenge)
        ) {
            challenge.nextChallenge(true);
        }
    }

    private void tickPlayerFreezing(ServerPlayerEntity player) {
        if (!state.running) { // then freeze
            boolean frozen = freezeStates.containsKey(player.getUuid());
            FreezeState freezeState = freezeStates.computeIfAbsent(
                    player.getUuid(),
                    (uuid) -> FreezeState.captureFromPlayer(player)
            );

            // freeze location
            if (player.getVehicle() != null) {
                player.dismountVehicle();
            }
            PlayerPosition teleportPosition = new PlayerPosition(
                    freezeState.position().position(),
                    Vec3d.ZERO,
                    freezeState.position().yaw(),
                    freezeState.position().pitch()
            );
            if (!PlayerPosition.fromEntity(player).equals(teleportPosition)) {
                player.networkHandler.requestTeleport(teleportPosition, Collections.emptySet());
            }

            // freeze interaction
            if (!frozen) player.changeGameMode(GameMode.ADVENTURE);

            // freeze damage
            if (!frozen) player.setStatusEffect(new StatusEffectInstance(
                    StatusEffects.RESISTANCE,
                    StatusEffectInstance.INFINITE,
                    StatusEffectInstance.MAX_AMPLIFIER,
                    false,
                    false
            ), null);

            // freeze modifiers
            EntityAttributeInstance gravity =
                    Objects.requireNonNull(player.getAttributeInstance(EntityAttributes.GRAVITY));
            EntityAttributeInstance oxygen =
                    Objects.requireNonNull(player.getAttributeInstance(EntityAttributes.OXYGEN_BONUS));
            if (!gravity.hasModifier(FREEZE_MODIFIER_ID)) {
                gravity.addPersistentModifier(new EntityAttributeModifier(
                        FREEZE_MODIFIER_ID,
                        -1,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                ));
            }
            if (!oxygen.hasModifier(FREEZE_MODIFIER_ID)) {
                oxygen.addPersistentModifier(new EntityAttributeModifier(
                        FREEZE_MODIFIER_ID,
                        Double.POSITIVE_INFINITY,
                        EntityAttributeModifier.Operation.ADD_VALUE
                ));
            }
        } else if (freezeStates.containsKey(player.getUuid())) { // then unfreeze
            // unfreeze location
            FreezeState freezeState = freezeStates.remove(player.getUuid());
            if (freezeState.vehicle() == null) {
                player.networkHandler.requestTeleport(freezeState.position(), Collections.emptySet());
            } else {
                player.startRiding(freezeState.vehicle(), true);
            }

            // unfreeze interaction
            player.changeGameMode(GameMode.SURVIVAL);

            // unfreeze damage
            if (freezeState.resistance() == null) {
                player.removeStatusEffect(StatusEffects.RESISTANCE);
            } else {
                player.setStatusEffect(freezeState.resistance(), null);
            }

            // unfreeze modifiers
            EntityAttributeInstance gravity =
                    Objects.requireNonNull(player.getAttributeInstance(EntityAttributes.GRAVITY));
            EntityAttributeInstance oxygen =
                    Objects.requireNonNull(player.getAttributeInstance(EntityAttributes.OXYGEN_BONUS));
            gravity.removeModifier(FREEZE_MODIFIER_ID);
            oxygen.removeModifier(FREEZE_MODIFIER_ID);
        }
    }

    private void sendTimer() {
        Text timerText = new Timer(state.duration - server.getOverworld().getTime())
                .toFormattedText(Formatting.GOLD, Formatting.RED, Formatting.BOLD);
        server.getPlayerManager().getPlayerList().forEach(player ->
                player.sendMessageToClient(timerText, true)
        );
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

    public record FreezeState(PlayerPosition position, Entity vehicle, StatusEffectInstance resistance) {
        public static FreezeState captureFromPlayer(ServerPlayerEntity player) {
            return new FreezeState(
                    PlayerPosition.fromEntity(player),
                    player.getVehicle(),
                    player.getStatusEffect(StatusEffects.RESISTANCE)
            );
        }
    }
}
