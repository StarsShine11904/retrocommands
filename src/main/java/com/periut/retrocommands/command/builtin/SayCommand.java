package com.periut.retrocommands.command.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.periut.retrocommands.command.RegistrationEnvironment;
import com.periut.retrocommands.command.RetroCommandSource;
import com.periut.retrocommands.text.Text;
import com.periut.retrocommands.util.ServerUtil;

import static com.periut.retrocommands.command.RetroCommandManager.argument;
import static com.periut.retrocommands.command.RetroCommandManager.literal;
import static com.periut.retrocommands.command.argument.MessageArgumentType.getMessage;
import static com.periut.retrocommands.command.argument.MessageArgumentType.message;

/** {@code /say <message>} - broadcast, with selectors in the message resolved to names. */
public final class SayCommand {
    private SayCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("say")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
            .then(argument("message", message())
                .executes(context -> {
                    final RetroCommandSource source = context.getSource();
                    final String text = "[" + source.getName() + "] " + getMessage(context, "message");

                    if (source.getServer() != null) {
                        ServerUtil.getConnectionManager().broadcast(text);
                    } else {
                        source.sendFeedback(Text.literal(text));
                    }
                    return Command.SINGLE_SUCCESS;
                })));
    }
}
