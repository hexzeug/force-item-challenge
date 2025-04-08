package com.hexzeug.forceitemchallenge.command;

import com.hexzeug.forceitemchallenge.ChallengeMaster;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.argument.TimeArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class TimerCommand {
    public static final SimpleCommandExceptionType SHORT_DURATION =
            new SimpleCommandExceptionType(Text.literal("Timer duration cannot be shorter than current gametime"));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("start").executes(context -> executeStart(context.getSource()))
        );
        dispatcher.register(
                literal("pause").executes(context -> executePause(context.getSource()))
        );
        dispatcher.register(
                literal("play").executes((context) -> executeStart(context.getSource()))
        );
        dispatcher.register(literal("timer")
                .executes(context -> executeGetTimer(context.getSource()))
                .then(literal("start").executes((context) -> executeStart(context.getSource())))
                .then(literal("play").executes((context) -> executeStart(context.getSource())))
                .then(literal("pause").executes((context) -> executePause(context.getSource())))
                .then(literal("duration")
                        .executes(context -> executeGetDuration(context.getSource()))
                        .then(argument("duration", TimeArgumentType.time())
                                .requires(source -> source.hasPermissionLevel(2))
                                .executes(context -> executeSetDuration(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "duration")
                                ))
                        )
                )
        );
    }

    private static int executeStart(ServerCommandSource source) {
        ChallengeMaster.ofServer(source.getServer()).start();
        return Command.SINGLE_SUCCESS;
    }

    private static int executePause(ServerCommandSource source) {
        ChallengeMaster.ofServer(source.getServer()).pause();
        return Command.SINGLE_SUCCESS;
    }

    private static int executeGetTimer(ServerCommandSource source) {
        source.sendFeedback(
                () -> Text.literal("Current timer is ")
                        .append(new ChallengeMaster.Timer(source.getServer().getOverworld().getTime()).toText()),
                false
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int executeGetDuration(ServerCommandSource source) {
        long duration = ChallengeMaster.ofServer(source.getServer()).getDuration();
        source.sendFeedback(
                () -> Text.literal("Current timer duration is ")
                        .append(new ChallengeMaster.Timer(duration).toText()),
                false
        );
        return (int) duration;
    }

    private static int executeSetDuration(ServerCommandSource source, long duration) throws CommandSyntaxException {
        if (ChallengeMaster.ofServer(source.getServer()).setDuration(duration)) {
            source.sendFeedback(
                    () -> Text.literal("Set timer duration to ")
                            .append(new ChallengeMaster.Timer(duration).toText()),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            throw SHORT_DURATION.create();
        }
    }
}
