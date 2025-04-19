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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.*;

public class Freezer {
    private static final Map<MinecraftServer, Freezer> instances = HashMap.newHashMap(1);
    private static final Identifier MODIFIER_ID = Identifier.of(ForceItemChallenge.MOD_NAMESPACE, "freeze");

    private final MinecraftServer server;
    private final Map<UUID, FreezeState> freezeStates;

    private Freezer(MinecraftServer server) {
        this.server = server;
        this.freezeStates = HashMap.newHashMap(16);
    }

    public static Freezer ofServer(MinecraftServer server) {
        return instances.computeIfAbsent(server, Freezer::new);
    }

    public void tick() {
        server.getPlayerManager().getPlayerList().forEach(this::tickPlayer);
    }

    private void tickPlayer(ServerPlayerEntity player) {
        if (server.getTickManager().isFrozen()) {
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
            if (!gravity.hasModifier(MODIFIER_ID)) {
                gravity.addPersistentModifier(new EntityAttributeModifier(
                        MODIFIER_ID,
                        -1,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                ));
            }
            if (!oxygen.hasModifier(MODIFIER_ID)) {
                oxygen.addPersistentModifier(new EntityAttributeModifier(
                        MODIFIER_ID,
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
            gravity.removeModifier(MODIFIER_ID);
            oxygen.removeModifier(MODIFIER_ID);
        }
    }

    private record FreezeState(PlayerPosition position, Entity vehicle, StatusEffectInstance resistance) {
        private static FreezeState captureFromPlayer(ServerPlayerEntity player) {
            StatusEffectInstance resistance = player.getStatusEffect(StatusEffects.RESISTANCE);
            if (resistance != null && resistance.isInfinite()) resistance = null;
            return new FreezeState(
                    PlayerPosition.fromEntity(player),
                    player.getVehicle(),
                    resistance
            );
        }
    }
}
