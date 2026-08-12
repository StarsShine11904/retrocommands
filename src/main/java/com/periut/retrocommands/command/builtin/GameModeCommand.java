package com.periut.retrocommands.command.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.periut.retrocommands.command.CommandUtil;
import com.periut.retrocommands.command.RegistrationEnvironment;
import com.periut.retrocommands.command.RetroCommandSource;
import com.periut.retrocommands.command.argument.GameModeArgumentType;
import com.periut.retrocommands.optionaldep.bhcreative.ChangeGamemode;
import com.periut.retrocommands.text.Text;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Collections;
import java.util.List;

import static com.periut.retrocommands.command.RetroCommandManager.argument;
import static com.periut.retrocommands.command.RetroCommandManager.literal;
import static com.periut.retrocommands.command.argument.EntityArgumentType.getPlayers;
import static com.periut.retrocommands.command.argument.EntityArgumentType.players;
import static com.periut.retrocommands.command.argument.GameModeArgumentType.gameMode;
import static com.periut.retrocommands.command.argument.GameModeArgumentType.getGameMode;

/**
 * {@code /gamemode <mode> [targets]} - registered only when BHCreative is installed, since it is
 * what gives beta a creative mode at all.
 */
public final class GameModeCommand {
    private GameModeCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("gamemode")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
            .then(argument("gamemode", gameMode())
                .executes(context -> set(context, Collections.singletonList(context.getSource().getPlayerOrThrow())))
                .then(argument("targets", players())
                    .executes(context -> set(context, getPlayers(context, "targets"))))));
    }

    private static int set(final CommandContext<RetroCommandSource> context, final List<PlayerEntity> targets) throws CommandSyntaxException {
        final int mode = getGameMode(context, "gamemode");

        for (final PlayerEntity target : targets) {
            ChangeGamemode.set(target, mode == GameModeArgumentType.CREATIVE);
        }

        context.getSource().sendFeedback(Text.literal("Set " + CommandUtil.joinNames(targets)
            + " to " + GameModeArgumentType.nameOf(mode) + " mode"));
        return Command.SINGLE_SUCCESS;
    }
}
