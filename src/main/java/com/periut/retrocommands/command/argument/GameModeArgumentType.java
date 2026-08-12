package com.periut.retrocommands.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.periut.retrocommands.command.SuggestionHelper;
import com.periut.retrocommands.text.Text;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * {@code survival} or {@code creative}, by name or by the numeric id modern Minecraft still accepts.
 * Beta has no adventure or spectator mode to offer.
 */
public class GameModeArgumentType implements ArgumentType<Integer> {
    private static final Collection<String> EXAMPLES = Arrays.asList("survival", "creative", "0", "1");
    private static final List<String> NAMES = List.of("survival", "creative");

    public static final DynamicCommandExceptionType UNKNOWN_GAME_MODE = new DynamicCommandExceptionType(
        mode -> Text.literal("Unknown game mode '" + mode + "'"));

    public static final int SURVIVAL = 0;
    public static final int CREATIVE = 1;

    private GameModeArgumentType() {
    }

    public static GameModeArgumentType gameMode() {
        return new GameModeArgumentType();
    }

    public static int getGameMode(final CommandContext<?> context, final String name) {
        return context.getArgument(name, Integer.class);
    }

    public static String nameOf(final int mode) {
        return mode == CREATIVE ? "creative" : "survival";
    }

    @Override
    public Integer parse(final StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        final String value = reader.readUnquotedString().toLowerCase(Locale.ROOT);

        return switch (value) {
            case "survival", "s", "0" -> SURVIVAL;
            case "creative", "c", "1" -> CREATIVE;
            default -> {
                reader.setCursor(start);
                throw UNKNOWN_GAME_MODE.createWithContext(reader, value);
            }
        };
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return SuggestionHelper.suggestMatching(NAMES, builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
