package com.periut.retrocommands.command;

import com.periut.retrocommands.command.argument.ItemIds;
import com.periut.retrocommands.command.argument.ItemNames;
import com.periut.retrocommands.command.argument.ItemStackArgument;
import com.periut.retrocommands.text.Formatting;
import com.periut.retrocommands.text.HoverEvent;
import com.periut.retrocommands.text.Text;
import com.periut.retrocommands.util.ServerUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.List;

/** Small operations several commands need, kept in one place rather than copied between them. */
public final class CommandUtil {
    private CommandUtil() {
    }

    /** Puts a stack in the first free slot. False - with a message already sent - if there is none. */
    public static boolean give(final RetroCommandSource source, final PlayerEntity player, final ItemStack stack) {
        final ItemStack[] inventory = player.inventory.main;
        for (int slot = 0; slot < inventory.length; slot++) {
            if (inventory[slot] == null) {
                inventory[slot] = stack;
                return true;
            }
        }

        source.sendError(Text.literal("Cannot give " + ItemNames.displayName(stack.itemId, stack.getDamage())
            + " because " + player.name + "'s inventory is full"));
        return false;
    }

    /**
     * Moves an entity.
     *
     * <p>A server has to go through the network handler so the client is told to move too;
     * singleplayer sets the position directly, and must clear velocity or the player keeps falling
     * at whatever speed they arrived with.
     */
    public static void teleport(final Entity entity, final Position position) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER && entity instanceof PlayerEntity player) {
            ServerUtil.serverTeleport(player, position.x(), position.y(), position.z());
            return;
        }

        entity.setPosition(position.x(), position.y(), position.z());
        entity.velocityX = 0.0;
        entity.velocityY = 0.0;
        entity.velocityZ = 0.0;
    }

    /**
     * An item as modern writes it in command output: its display name in square brackets, left the
     * default colour, with the identifier and numeric id on hover for anyone who needs them.
     */
    public static Text describeItem(final ItemStackArgument item) {
        final String identifier = ItemIds.nameOf(item.itemId())
            + (item.meta() == 0 ? "" : ":" + item.meta());

        return Text.literal("[" + ItemNames.displayName(item.itemId(), item.meta()) + "]")
            .styled(style -> style.withHoverEvent(HoverEvent.showText(
                Text.literal(identifier).formatted(Formatting.GRAY)
                    .append(Text.literal("\nid " + item.itemId()).formatted(Formatting.DARK_GRAY)))));
    }

    /** "a, b and c" - how modern phrases a list of affected targets. */
    public static String joinNames(final List<? extends Entity> entities) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < entities.size(); i++) {
            if (i > 0) {
                builder.append(i == entities.size() - 1 ? " and " : ", ");
            }
            builder.append(com.periut.retrocommands.command.selector.EntitySelectorReader.nameOf(entities.get(i)));
        }
        return builder.toString();
    }
}
