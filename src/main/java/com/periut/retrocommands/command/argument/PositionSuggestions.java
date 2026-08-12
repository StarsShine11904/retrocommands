package com.periut.retrocommands.command.argument;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.periut.retrocommands.command.SuggestionHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Completions for a three-coordinate argument, following modern Minecraft's rule exactly: offer the
 * position one coordinate at a time, growing as each is filled in.
 *
 * <p>An empty argument offers {@code ~}, {@code ~ ~} and {@code ~ ~ ~}; with one coordinate typed it
 * offers that coordinate plus the remaining one or two. Local ({@code ^}) coordinates are accepted
 * by the parser but never suggested, which is also what modern does - they are a specialist tool and
 * would double the size of the list.
 */
final class PositionSuggestions {
    private PositionSuggestions() {
    }

    static CompletableFuture<Suggestions> suggest(final SuggestionsBuilder builder, final boolean blockPos) {
        final String remaining = builder.getRemaining();
        final List<String> candidates = new ArrayList<>(3);

        if (remaining.isEmpty()) {
            candidates.add("~");
            candidates.add("~ ~");
            candidates.add("~ ~ ~");
        } else {
            // Splitting without a negative limit drops the trailing empty piece, so "~ " counts as
            // one coordinate typed rather than two - joining an empty one back in would offer "~  ~".
            final String[] typed = remaining.split(" ");
            if (typed.length == 1) {
                candidates.add(typed[0] + " ~");
                candidates.add(typed[0] + " ~ ~");
            } else if (typed.length == 2) {
                candidates.add(typed[0] + " " + typed[1] + " ~");
            }
        }

        return SuggestionHelper.suggestMatching(candidates, builder);
    }
}
