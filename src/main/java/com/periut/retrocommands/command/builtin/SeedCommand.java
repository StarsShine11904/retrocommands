package com.periut.retrocommands.command.builtin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.periut.retrocommands.command.RegistrationEnvironment;
import com.periut.retrocommands.command.RetroCommandSource;
import com.periut.retrocommands.text.ClickEvent;
import com.periut.retrocommands.text.Formatting;
import com.periut.retrocommands.text.HoverEvent;
import com.periut.retrocommands.text.Text;
import net.minecraft.world.World;

import static com.periut.retrocommands.command.RetroCommandManager.literal;

/** {@code /seed}, with modern's click-to-copy on the number. */
public final class SeedCommand {
    private SeedCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("seed")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
            .executes(context -> {
                final RetroCommandSource source = context.getSource();
                final World world = source.getWorld();
                if (world == null) {
                    throw RetroCommandSource.REQUIRES_PLAYER.create();
                }

                final long seed = world.getSeed();
                source.sendFeedback(Text.literal("Seed: ").append(
                    Text.literal("[" + seed + "]")
                        .formatted(Formatting.GREEN)
                        .styled(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, String.valueOf(seed)))
                            .withHoverEvent(HoverEvent.showText(Text.literal("Click to copy to clipboard"))))));
                return (int) seed;
            }));
    }
}
