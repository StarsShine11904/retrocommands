package com.periut.retrocommands.command.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.periut.retrocommands.command.RegistrationEnvironment;
import com.periut.retrocommands.command.RetroCommandSource;
import com.periut.retrocommands.text.Text;
import com.periut.retrocommands.util.ServerUtil;

import static com.periut.retrocommands.command.RetroCommandManager.argument;
import static com.periut.retrocommands.command.RetroCommandManager.literal;
import static com.periut.retrocommands.command.argument.MessageArgumentType.getMessage;
import static com.periut.retrocommands.command.argument.MessageArgumentType.message;

/** {@code /me <action>} - available to everyone, as in modern. */
public final class MeCommand {
    private MeCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("me")
            .then(argument("action", message())
                .executes(context -> {
                    final RetroCommandSource source = context.getSource();
                    final String text = "* " + source.getName() + " " + getMessage(context, "action");

                    if (source.getServer() != null) {
                        ServerUtil.getConnectionManager().broadcast(text);
                    } else {
                        source.sendFeedback(Text.literal(text));
                    }
                    return Command.SINGLE_SUCCESS;
                })));
    }
}
