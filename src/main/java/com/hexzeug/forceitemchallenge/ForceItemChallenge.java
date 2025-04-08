package com.hexzeug.forceitemchallenge;

import com.hexzeug.forceitemchallenge.command.NewCommand;
import com.hexzeug.forceitemchallenge.command.SkipCommand;
import com.hexzeug.forceitemchallenge.command.TimerCommand;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForceItemChallenge implements ModInitializer {
	public static final String MOD_ID = "force-item-challenge";
	public static final String MOD_NAMESPACE = "force_item_challenge";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register(this::registerCommands);
		ServerLifecycleEvents.SERVER_STARTED.register(this::onServerTick);
		ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
	}

	private void registerCommands(
			CommandDispatcher<ServerCommandSource> dispatcher,
			CommandRegistryAccess registryAccess,
			CommandManager.RegistrationEnvironment environment
	) {
		TimerCommand.register(dispatcher);
		SkipCommand.register(dispatcher);
		if (environment.dedicated) {
			NewCommand.register(dispatcher);
		}
	}

	private void onServerTick(MinecraftServer server) {
		ChallengeMaster.ofServer(server).tick();
	}
}