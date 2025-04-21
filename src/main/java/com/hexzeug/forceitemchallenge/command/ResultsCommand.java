package com.hexzeug.forceitemchallenge.command;

import com.google.common.collect.Sets;
import com.hexzeug.forceitemchallenge.display.ResultsDisplay;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Set;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ResultsCommand {
    public static final SimpleCommandExceptionType ALREADY_SHOWN_EXCEPTION =
            new SimpleCommandExceptionType(Text.literal("All results were already shown"));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("results")
                        .requires((source) -> source.hasPermissionLevel(2))
                        .executes(context -> next(
                                context.getSource()
                        ))
                        .then(argument("player", EntityArgumentType.player())
                                .executes(context -> open(
                                        context.getSource(),
                                        EntityArgumentType.getPlayer(context, "player")
                                ))
                        )
        );
    }

    private static final Set<UUID> opened = Sets.newHashSet();

    private static int next(ServerCommandSource source) throws CommandSyntaxException {
        ResultsDisplay display = null;
        for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
            if (opened.contains(player.getUuid())) continue;
            ResultsDisplay playerDisplay = new ResultsDisplay(player);
            if (display == null || display.getScore().total() > playerDisplay.getScore().total()) {
                display = playerDisplay;
            }
        }
        if (display == null) throw ALREADY_SHOWN_EXCEPTION.create();

        open(source.getServer(), display);
        int place = source.getServer().getCurrentPlayerCount() - opened.size();
        opened.add(display.getPlayer().getUuid());
        source.sendFeedback(() -> Text.literal("Showing results of place " + place), false);
        source.getServer().getPlayerManager().broadcast(
                Text
                        .literal("#" + place + " ")
                        .append(display.getPlayer().getName())
                        .formatted(Formatting.DARK_AQUA, Formatting.BOLD),
                false
        );
        return source.getServer().getPlayerManager().getCurrentPlayerCount() - opened.size();
    }

    private static int open(ServerCommandSource source, ServerPlayerEntity player) {
        open(source.getServer(), new ResultsDisplay(player));
        source.sendFeedback(() -> Text.literal("Showing results of ").append(player.getName()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static void open(MinecraftServer server, ResultsDisplay display) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.openHandledScreen(display);
        }
    }
}
