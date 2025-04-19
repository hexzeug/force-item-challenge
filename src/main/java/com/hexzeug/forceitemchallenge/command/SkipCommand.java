package com.hexzeug.forceitemchallenge.command;

import com.hexzeug.forceitemchallenge.Challenger;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.argument.*;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SkipCommand {
    public static final SimpleCommandExceptionType NO_CHALLENGE_EXCEPTION =
            new SimpleCommandExceptionType(Text.literal("Player has no challenge"));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("skip")
                        .executes(context -> execute(
                                context.getSource()
                        ))
                        .then(
                                argument("player", EntityArgumentType.player())
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(context -> execute(
                                                context.getSource(),
                                                EntityArgumentType.getPlayer(context, "player")
                                        ))
                        )
        );
    }

    public static int execute(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        Challenger challenger = Challenger.ofServer(source.getServer());
        ItemStack oldChallenge = challenger.getChallenge(player).orElseThrow(NO_CHALLENGE_EXCEPTION::create);
        challenger.skipChallengeOf(player);
        source.sendFeedback(
                () -> Text.literal("Skipped challenge ").append(oldChallenge.toHoverableText()),
                false
        );
        return 1;
    }

    public static int execute(ServerCommandSource source, ServerPlayerEntity player) throws CommandSyntaxException {
        Challenger challenger = Challenger.ofServer(source.getServer());
        ItemStack oldChallenge = challenger.getChallenge(player).orElseThrow(NO_CHALLENGE_EXCEPTION::create);
        challenger.silentlySkipChallengeOf(player);
        source.sendFeedback(
                () -> Text
                        .literal("Skipped challenge ")
                        .append(oldChallenge.toHoverableText())
                        .append(" of ")
                        .append(player.getName()),
                true
        );
        return Command.SINGLE_SUCCESS;
    }
}
