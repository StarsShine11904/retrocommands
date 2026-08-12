package com.periut.retrocommands.command.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.periut.retrocommands.command.CommandUtil;
import com.periut.retrocommands.command.Position;
import com.periut.retrocommands.command.RegistrationEnvironment;
import com.periut.retrocommands.command.RetroCommandManager;
import com.periut.retrocommands.command.RetroCommandSource;
import com.periut.retrocommands.optionaldep.stapi.SwitchDimension;
import com.periut.retrocommands.text.Text;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Collections;
import java.util.List;

import static com.periut.retrocommands.command.RetroCommandManager.argument;
import static com.periut.retrocommands.command.RetroCommandManager.literal;
import static com.periut.retrocommands.command.argument.DimensionArgumentType.dimension;
import static com.periut.retrocommands.command.argument.DimensionArgumentType.getDimension;
import static com.periut.retrocommands.command.argument.EntityArgumentType.entities;
import static com.periut.retrocommands.command.argument.EntityArgumentType.entity;
import static com.periut.retrocommands.command.argument.EntityArgumentType.getEntities;
import static com.periut.retrocommands.command.argument.EntityArgumentType.getEntity;
import static com.periut.retrocommands.command.argument.Vec3ArgumentType.getVec3;
import static com.periut.retrocommands.command.argument.Vec3ArgumentType.vec3;

/**
 * {@code /teleport}, aliased to {@code /tp}, in modern Minecraft's four shapes: move yourself to a
 * location or an entity, or move others to either.
 *
 * <p>The trailing {@code [dimension]} is a beta-only extension standing in for modern's
 * {@code /execute in <dimension> run teleport}, which needs machinery beta has no equivalent of. It
 * requires StationAPI's dimension module; without it the command says so rather than silently
 * teleporting within the current dimension.
 */
public final class TeleportCommand {
    private TeleportCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        final LiteralCommandNode<RetroCommandSource> node = dispatcher.register(literal("teleport")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
            .then(argument("location", vec3())
                .executes(context -> teleportToPosition(context, Collections.singletonList(context.getSource().getEntityOrThrow()), getVec3(context, "location"), null))
                .then(argument("dimension", dimension())
                    .executes(context -> teleportToPosition(context, Collections.singletonList(context.getSource().getEntityOrThrow()), getVec3(context, "location"), getDimension(context, "dimension")))))
            .then(argument("destination", entity())
                .executes(context -> teleportToEntity(context, Collections.singletonList(context.getSource().getEntityOrThrow()), getEntity(context, "destination"))))
            .then(argument("targets", entities())
                .then(argument("location", vec3())
                    .executes(context -> teleportToPosition(context, getEntities(context, "targets"), getVec3(context, "location"), null))
                    .then(argument("dimension", dimension())
                        .executes(context -> teleportToPosition(context, getEntities(context, "targets"), getVec3(context, "location"), getDimension(context, "dimension")))))
                .then(argument("destination", entity())
                    .executes(context -> teleportToEntity(context, getEntities(context, "targets"), getEntity(context, "destination"))))));

        RetroCommandManager.alias(dispatcher, node, "tp");
    }

    private static int teleportToPosition(final CommandContext<RetroCommandSource> context, final List<? extends Entity> targets,
                                          final Position position, final String dimensionId) throws CommandSyntaxException {
        final RetroCommandSource source = context.getSource();

        if (dimensionId != null && !switchDimension(source, targets, dimensionId)) {
            return 0;
        }

        for (final Entity target : targets) {
            CommandUtil.teleport(target, position);
        }

        source.sendFeedback(Text.literal("Teleported " + CommandUtil.joinNames(targets) + " to " + position));
        return Command.SINGLE_SUCCESS;
    }

    private static int teleportToEntity(final CommandContext<RetroCommandSource> context, final List<? extends Entity> targets,
                                        final Entity destination) throws CommandSyntaxException {
        final RetroCommandSource source = context.getSource();

        for (final Entity target : targets) {
            // Crossing dimensions to reach the destination needs StationAPI; without it the target
            // would be moved to coordinates in a world they are not in.
            if (needsDimensionChange(target, destination)
                && !switchDimension(source, Collections.singletonList(target), null)) {
                return 0;
            }
            CommandUtil.teleport(target, new Position(destination.x, destination.y, destination.z));
        }

        source.sendFeedback(Text.literal("Teleported " + CommandUtil.joinNames(targets) + " to "
            + com.periut.retrocommands.command.selector.EntitySelectorReader.nameOf(destination)));
        return Command.SINGLE_SUCCESS;
    }

    /** Only players carry a dimension in beta, and only they can be moved between them. */
    private static boolean needsDimensionChange(final Entity target, final Entity destination) {
        return target instanceof PlayerEntity player
            && destination instanceof PlayerEntity other
            && player.dimensionId != other.dimensionId;
    }

    private static boolean switchDimension(final RetroCommandSource source, final List<? extends Entity> targets, final String dimensionId) {
        if (!FabricLoader.getInstance().isModLoaded("station-dimensions-v0")) {
            source.sendError(Text.literal("Changing dimension requires StationAPI's dimension module"));
            return false;
        }

        for (final Entity target : targets) {
            if (!(target instanceof PlayerEntity player)) {
                source.sendError(Text.literal("Only players can change dimension"));
                return false;
            }
            if (dimensionId != null && !SwitchDimension.go(player, dimensionId)) {
                source.sendError(Text.literal("Unknown dimension: " + dimensionId));
                return false;
            }
        }

        return true;
    }
}
