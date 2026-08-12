package com.periut.retrocommands.command.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.periut.retrocommands.client.gui.RetroChatHud;
import com.periut.retrocommands.command.RegistrationEnvironment;
import com.periut.retrocommands.command.RetroCommandSource;
import com.periut.retrocommands.text.Text;

import static com.periut.retrocommands.command.RetroCommandManager.literal;

/**
 * {@code /clearchat} - what {@code /clear} used to do here before modern's inventory-clearing
 * {@code /clear} took the name.
 */
public final class ClearChatCommand {
    private ClearChatCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        // Chat only exists on a client; on a server there is nothing to clear.
        if (environment.isDedicated()) {
            return;
        }

        dispatcher.register(literal("clearchat")
            .executes(context -> {
                RetroChatHud.getInstance().clear();
                context.getSource().sendFeedback(Text.literal("Cleared chat"));
                return Command.SINGLE_SUCCESS;
            }));
    }
}
