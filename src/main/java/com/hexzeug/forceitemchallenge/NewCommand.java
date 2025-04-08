package com.hexzeug.forceitemchallenge;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static net.minecraft.server.command.CommandManager.literal;

public class NewCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("new")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes((context) -> execute(context.getSource().getServer()))
        );
    }

    public static int execute(MinecraftServer server) {
        new WorldRemoveThread(server).start();
        return 0;
    }

    public static class WorldRemoveThread extends Thread {
        private final MinecraftServer server;

        public WorldRemoveThread(MinecraftServer server) {
            super("RemoveWorld");
            this.server = server;
        }

        @Override
        public void run() {
            Path levelDir = Path.of(server.getOverworld().getChunkManager().chunkLoadingManager.getSaveDir());
            server.getPlayerManager().getPlayerList().forEach(player ->
                    player.networkHandler.disconnect(Text.literal("Creating new world. Please rejoin."))
            );
            ForceItemChallenge.LOGGER.info("Stopping server...");
            server.stop(true);
            ForceItemChallenge.LOGGER.info("Server stopped. Deleting world...");
            try (Stream<Path> stream = Files.walk(levelDir)) {
                stream
                        .skip(1)
                        .sorted(Comparator.reverseOrder())
                        .forEachOrdered(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException ex) {
                                ForceItemChallenge.LOGGER.error("Failed deleting {} from world", path, ex);
                            }
                        });
                ForceItemChallenge.LOGGER.info("World deleted");
            } catch (IOException ex) {
                ForceItemChallenge.LOGGER.error("Failed deleting world {}", levelDir, ex);
            }
        }
    }
}
