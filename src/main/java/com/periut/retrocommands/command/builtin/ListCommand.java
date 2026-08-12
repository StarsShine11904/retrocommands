package com.periut.retrocommands.command.builtin;

import com.mojang.brigadier.CommandDispatcher;
import com.periut.retrocommands.command.RegistrationEnvironment;
import com.periut.retrocommands.command.RetroCommandSource;
import com.periut.retrocommands.text.Text;

import java.util.List;

import static com.periut.retrocommands.command.RetroCommandManager.literal;

/** {@code /list} - who is online. */
public final class ListCommand {
    private ListCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("list")
            .executes(context -> {
                final RetroCommandSource source = context.getSource();
                final List<String> names = source.getPlayerNames();

                source.sendFeedback(Text.literal("There are " + names.size() + " players online:"));
                source.sendFeedback(Text.literal(String.join(", ", names)));
                return names.size();
            }));
    }
}
