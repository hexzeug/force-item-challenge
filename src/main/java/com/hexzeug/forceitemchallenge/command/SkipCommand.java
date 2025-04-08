package com.hexzeug.forceitemchallenge.command;

import com.hexzeug.forceitemchallenge.Challenge;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.argument.*;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SkipCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("skip")
                        .requires((source) -> source.hasPermissionLevel(2))
                        .executes((context) -> execute(
                                context.getSource(),
                                context.getSource().getPlayerOrThrow()
                        ))
                        .then(
                                argument("players", EntityArgumentType.players())
                                        .executes((context) -> execute(
                                                context.getSource(),
                                                EntityArgumentType.getPlayers(context, "players")
                                        ))
                        )
        );
    }

    public static int execute(ServerCommandSource source, ServerPlayerEntity player) {
        Challenge.ofPlayer(player).nextChallenge(false);
        source.sendFeedback(
                () -> Text.literal("Skipped your challenge"),
                true
        );
        return 1;
    }

    public static int execute(ServerCommandSource source, Collection<ServerPlayerEntity> players) {
        int amount = players.size();
        players.forEach(player -> Challenge.ofPlayer(player).nextChallenge(false));
        source.sendFeedback(
                () -> Text.literal("Skipped challenges of %s players".formatted(amount)),
                true
        );
        return amount;

    }
}
