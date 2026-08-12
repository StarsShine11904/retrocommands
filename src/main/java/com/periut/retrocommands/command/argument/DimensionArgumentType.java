package com.periut.retrocommands.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.periut.retrocommands.command.SuggestionHelper;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A dimension identifier.
 *
 * <p>Beta has no dimension registry of its own, so the suggestions come from StationAPI when it is
 * installed and fall back to the two dimensions vanilla ships. The value is validated at execution
 * time by whoever performs the transfer, not here - a dimension can be added by another mod after
 * the command tree was built.
 */
public class DimensionArgumentType implements ArgumentType<String> {
    private static final Collection<String> EXAMPLES = Arrays.asList("minecraft:overworld", "minecraft:the_nether");
    private static final List<String> VANILLA = List.of("minecraft:overworld", "minecraft:the_nether");

    private DimensionArgumentType() {
    }

    public static DimensionArgumentType dimension() {
        return new DimensionArgumentType();
    }

    public static String getDimension(final CommandContext<?> context, final String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(final StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        while (reader.canRead() && (StringReader.isAllowedInUnquotedString(reader.peek()) || reader.peek() == ':')) {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return SuggestionHelper.suggestIdentifiers(dimensionIds(), builder);
    }

    public static List<String> dimensionIds() {
        if (FabricLoader.getInstance().isModLoaded("station-dimensions-v0")) {
            final List<String> ids = stationDimensionIds();
            if (!ids.isEmpty()) {
                return ids;
            }
        }
        return VANILLA;
    }

    /** StationAPI types stay behind this call so they load only when the mod is present. */
    private static List<String> stationDimensionIds() {
        try {
            final List<String> ids = new java.util.ArrayList<>();
            for (final net.modificationstation.stationapi.api.util.Identifier id
                : net.modificationstation.stationapi.api.registry.DimensionRegistry.INSTANCE.getIds()) {
                ids.add(id.namespace.toString() + ":" + id.path);
            }
            ids.sort(String::compareTo);
            return ids;
        } catch (final RuntimeException | LinkageError ignored) {
            return List.of();
        }
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
