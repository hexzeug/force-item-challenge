package com.hexzeug.forceitemchallenge;

import com.hexzeug.forceitemchallenge.command.NewCommand;
import com.hexzeug.forceitemchallenge.command.SkipCommand;
import com.hexzeug.forceitemchallenge.command.TimerCommand;
import com.hexzeug.forceitemchallenge.display.DisplayTimer;
import com.hexzeug.forceitemchallenge.persistence.ForceItemChallengeState;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
		ServerPlayConnectionEvents.JOIN.register(this::onJoin);
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
		ForceItemChallengeState state = ForceItemChallengeState.ofServer(server);

		if (state.isRunning() && server.getOverworld().getTime() < state.getDuration()) {
			Challenger.ofServer(server).tick();
		}
		Freezer.ofServer(server).tick(!state.isRunning());

		if ((state.isRunning() ? server.getOverworld().getTime() : server.getTicks()) % 20 == 0) {
			Text timerText = new DisplayTimer(state.getDuration() - server.getOverworld().getTime())
					.toFormattedText(Formatting.GOLD, Formatting.RED, Formatting.BOLD);
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				player.sendMessageToClient(timerText, true);
			}
		}
		if (server.getOverworld().getTime() == state.getDuration()) {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				player.playSoundToPlayer(
						SoundEvents.ENTITY_WITHER_SPAWN,
						SoundCategory.MASTER,
						100,
						1.0f
				);
			}
		}
	}

	private void onJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
		ServerPlayerEntity player = handler.getPlayer();
		Challenger challenger = Challenger.ofServer(server);
		challenger.getChallenge(player).ifPresent(challenge ->
				challenger.challengeDisplay(player).updateChallenge(challenge)
		);
	}
}