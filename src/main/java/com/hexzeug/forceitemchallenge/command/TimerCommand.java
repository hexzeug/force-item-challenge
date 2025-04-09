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
    public static final SimpleCommandExceptionType ALREADY_RUNNING =
            new SimpleCommandExceptionType(Text.literal("Timer is already running"));
    public static final SimpleCommandExceptionType ALREADY_PAUSED =
            new SimpleCommandExceptionType(Text.literal("Timer is already paused"));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("pause").executes(context -> executePause(context.getSource()))
        );
        dispatcher.register(
                literal("play").executes(context -> executeStart(context.getSource()))
        );
        dispatcher.register(literal("timer")
                .then(literal("query").executes(context -> executeGetTimer(context.getSource())))
                .then(literal("resume").executes(context -> executeStart(context.getSource())))
                .then(literal("pause").executes(context -> executePause(context.getSource())))
                .then(literal("duration")
                        .then(literal("query")
                                .executes(context -> executeGetDuration(context.getSource()))
                        )
                        .then(literal("set")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(argument("duration", TimeArgumentType.time())
                                        .executes(context -> executeSetDuration(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "duration")
                                        ))
                                )
                        )
                        .then(literal("add")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(argument("duration", TimeArgumentType.time(Integer.MIN_VALUE))
                                        .executes(context -> executeAddDuration(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "duration")
                                        ))
                                )
                        )
                )
        );
    }

    private static int executeStart(ServerCommandSource source) throws CommandSyntaxException {
        if (ChallengeMaster.ofServer(source.getServer()).start()) {
            source.sendFeedback(() -> Text.literal("Resumed timer"), true);
            return Command.SINGLE_SUCCESS;
        } else {
            throw ALREADY_RUNNING.create();
        }
    }

    private static int executePause(ServerCommandSource source) throws CommandSyntaxException {
        if (ChallengeMaster.ofServer(source.getServer()).pause()) {
            source.sendFeedback(() -> Text.literal("Paused timer"), true);
            return Command.SINGLE_SUCCESS;
        } else {
            throw ALREADY_PAUSED.create();
        }
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

    private static int executeAddDuration(ServerCommandSource source, int ticks) throws CommandSyntaxException {
        long duration = ChallengeMaster.ofServer(source.getServer()).getDuration();
        return executeSetDuration(source, duration + ticks);
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
