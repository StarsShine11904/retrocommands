package com.periut.retrocommands.command.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.periut.retrocommands.command.RegistrationEnvironment;
import com.periut.retrocommands.command.RetroCommandSource;
import com.periut.retrocommands.text.Text;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import static com.periut.retrocommands.command.RetroCommandManager.literal;

/** {@code /hat} - swaps the held item with whatever is on your head. */
public final class HatCommand {
    private static final int HELMET_SLOT = 3;

    private HatCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("hat")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
            .executes(context -> {
                final RetroCommandSource source = context.getSource();
                final PlayerEntity player = source.getPlayerOrThrow();

                final ItemStack held = player.inventory.getSelectedItem() == null ? null : player.inventory.getSelectedItem().copy();
                final ItemStack worn = player.inventory.armor[HELMET_SLOT] == null ? null : player.inventory.armor[HELMET_SLOT].copy();

                player.inventory.armor[HELMET_SLOT] = held;
                player.inventory.main[player.inventory.selectedSlot] = worn;

                source.sendFeedback(Text.literal("Swapped hat with hand"));
                return Command.SINGLE_SUCCESS;
            }));
    }
}
