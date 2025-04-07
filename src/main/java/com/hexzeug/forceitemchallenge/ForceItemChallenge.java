package com.hexzeug.forceitemchallenge;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class ForceItemChallenge implements ModInitializer {
	public static final String MOD_ID = "force-item-challenge";
	public static final String MOD_NAMESPACE = "force_item_challenge";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
	}

	private void onServerTick(MinecraftServer server) {
		server.getWorlds().forEach((world) -> world.getPlayers().forEach(this::tickChallenge));
	}

	private void tickChallenge(ServerPlayerEntity player) {
		MinecraftServer server = Objects.requireNonNull(player.getServer());
		ForceItemState state = ForceItemState.getServerState(server);
		ForceItemState.PlayerData playerData = state.getPlayerData(player);

		if (player.getInventory().contains(playerData::isChallenge)) {
			Identifier randomId = Identifier.of(MOD_NAMESPACE, "challenge/" + player.getNameForScoreboard());
			Random random = server.getOverworld().getOrCreateRandom(randomId);
			playerData.newChallenge(random);
			player.sendMessageToClient(playerData.getChallenge().getName(), false);
		};
	}
}