package com.hexzeug.forceitemchallenge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ForceItemChallenge implements ModInitializer {
	public static final String MOD_ID = "force-item-challenge";
	public static final String MOD_NAMESPACE = "force_item_challenge";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register(this::registerCommands);
		ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
	}

	private void registerCommands(
			CommandDispatcher<ServerCommandSource> dispatcher,
			CommandRegistryAccess registryAccess,
			CommandManager.RegistrationEnvironment environment
	) {
		SkipCommand.register(dispatcher);
	}

	private void onServerTick(MinecraftServer server) {
		server.getWorlds().forEach((world) -> world.getPlayers().forEach(this::tickChallenge));
	}

	private void tickChallenge(ServerPlayerEntity player) {
		Challenge challenge = Challenge.ofPlayer(player);

		if (player.getInventory().contains(challenge::isChallenge)) {
			challenge.nextChallenge(true);
		};
	}
}