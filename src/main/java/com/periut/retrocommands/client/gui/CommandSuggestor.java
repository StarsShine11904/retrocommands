package com.periut.retrocommands.client.gui;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;
import com.periut.retrocommands.client.ClientCommands;
import com.periut.retrocommands.command.RetroCommandSource;
import com.periut.retrocommands.text.Formatting;
import com.periut.retrocommands.text.Style;
import com.periut.retrocommands.text.Text;
import com.periut.retrocommands.text.Texts;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * The completion window and the coloured command line, backported from modern Minecraft.
 *
 * <p>Every edit re-parses the input. What the parser understood is coloured argument by argument;
 * what it choked on is underlined in red with the error printed beneath, and whatever could come
 * next is offered in a scrollable window driven by the arrow keys, {@code Tab}, {@code Enter}, the
 * mouse and the wheel.
 */
public class CommandSuggestor extends DrawContext {
    /** Modern cycles these across successive arguments so each one is visually distinct. */
    private static final Style[] HIGHLIGHT_STYLES = {
        Style.EMPTY.withColor(Formatting.AQUA),
        Style.EMPTY.withColor(Formatting.YELLOW),
        Style.EMPTY.withColor(Formatting.GREEN),
        Style.EMPTY.withColor(Formatting.LIGHT_PURPLE),
        Style.EMPTY.withColor(Formatting.GOLD),
    };
    private static final Style ERROR_STYLE = Style.EMPTY.withColor(Formatting.RED);
    private static final Style PLAIN_STYLE = Style.EMPTY;

    private static final int MAX_VISIBLE = 10;
    private static final int ENTRY_HEIGHT = 12;
    private static final int BACKGROUND = 0xD0000000;
    private static final int SELECTED_COLOR = 0xFFFF00;
    private static final int UNSELECTED_COLOR = 0xAAAAAA;

    private final RetroChatScreen owner;
    private final ChatInputField input;
    private final TextRenderer textRenderer;

    private ParseResults<RetroCommandSource> parse;
    private CompletableFuture<Suggestions> pendingSuggestions;
    private Window window;
    private boolean completing;
    /** Messages under the input box: a parse error, or the usage list Tab produces. */
    private final List<Text> messages = new ArrayList<>();

    public CommandSuggestor(final RetroChatScreen owner, final ChatInputField input, final TextRenderer textRenderer) {
        this.owner = owner;
        this.input = input;
        this.textRenderer = textRenderer;
    }

    /** Re-parses and re-requests completions; called whenever the text changes. */
    public void refresh() {
        final String text = input.getText();

        if (parse != null && !parse.getReader().getString().equals(text)) {
            parse = null;
        }
        if (!completing) {
            input.setSuggestion("");
            window = null;
        }
        messages.clear();

        if (!text.startsWith("/")) {
            parse = null;
            pendingSuggestions = null;
            return;
        }

        final StringReader reader = new StringReader(text);
        reader.skip();

        if (parse == null) {
            parse = ClientCommands.parse(reader);
        }

        final int cursor = input.getCursor();
        if (cursor >= 1 && (window == null || !completing)) {
            // The reader was positioned after the slash but still spans the whole input, so every
            // range - and the cursor - is an index into the text as typed.
            pendingSuggestions = ClientCommands.suggest(parse, cursor);
            final CompletableFuture<Suggestions> requested = pendingSuggestions;
            requested.thenRun(() -> {
                if (requested == pendingSuggestions && requested.isDone()) {
                    show();
                }
            });
        }
    }

    private void show() {
        if (pendingSuggestions == null || !pendingSuggestions.isDone()) {
            return;
        }

        final Suggestions suggestions = pendingSuggestions.join();

        if (input.getCursor() == input.getText().length()) {
            describeErrors(suggestions);
        }

        if (suggestions.isEmpty()) {
            window = null;
            return;
        }

        window = new Window(suggestions);
    }

    /** Explains why a command will not run, using Brigadier's own message and position. */
    private void describeErrors(final Suggestions suggestions) {
        if (parse == null) {
            return;
        }

        if (suggestions.isEmpty() && !parse.getExceptions().isEmpty()) {
            for (final Map.Entry<CommandNode<RetroCommandSource>, CommandSyntaxException> entry : parse.getExceptions().entrySet()) {
                messages.add(Text.literal(entry.getValue().getRawMessage().getString()).formatted(Formatting.RED));
            }
            return;
        }

        if (parse.getReader().canRead()) {
            messages.add(Text.literal(parse.getContext().getRange().isEmpty()
                ? "Unknown command"
                : "Incorrect argument for command").formatted(Formatting.RED));
        }
    }

    /**
     * @return true if the key was consumed by the suggestion window
     */
    public boolean keyPressed(final char typed, final int keyCode) {
        if (window != null) {
            switch (keyCode) {
                case Keys.UP -> {
                    window.select(window.selection - 1);
                    return true;
                }
                case Keys.DOWN -> {
                    window.select(window.selection + 1);
                    return true;
                }
                case Keys.TAB, Keys.RETURN, Keys.NUMPAD_ENTER -> {
                    window.complete();
                    return true;
                }
                case Keys.ESCAPE -> {
                    window = null;
                    return true;
                }
                default -> {
                    return false;
                }
            }
        }

        // Tab with nothing to complete lists what the command actually accepts, as modern does.
        if (keyCode == Keys.TAB) {
            showUsages();
            return true;
        }

        return false;
    }

    private void showUsages() {
        if (parse == null || !input.getText().startsWith("/")) {
            return;
        }

        messages.clear();

        final CommandContextBuilder<RetroCommandSource> context = parse.getContext().getLastChild();
        final List<ParsedCommandNode<RetroCommandSource>> nodes = context.getNodes();
        final CommandNode<RetroCommandSource> node = nodes.isEmpty()
            ? ClientCommands.getDispatcher().getRoot()
            : nodes.get(nodes.size() - 1).getNode();

        final String[] usages = ClientCommands.getDispatcher().getAllUsage(node, ClientCommands.getSource(), true);
        for (final String usage : usages) {
            messages.add(Text.literal(usage).formatted(Formatting.GRAY));
        }
        if (usages.length == 0) {
            messages.add(Text.literal("No further arguments").formatted(Formatting.GRAY));
        }
    }

    public boolean mouseClicked(final int mouseX, final int mouseY, final int button) {
        return window != null && window.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseScrolled(final int amount) {
        if (window == null) {
            return false;
        }
        window.scroll(amount);
        return true;
    }

    public void render(final int mouseX, final int mouseY) {
        if (window != null) {
            window.render(mouseX, mouseY);
            return;
        }

        // Messages sit where the window would have been, so the two never overlap.
        int y = owner.height - 14 - messages.size() * ENTRY_HEIGHT;
        for (final Text message : messages) {
            final TextDrawer.Line line = TextDrawer.flatten(message);
            final int width = TextDrawer.INSTANCE.width(textRenderer, line);
            fill(2, y - 1, 4 + width, y + ENTRY_HEIGHT - 3, BACKGROUND);
            TextDrawer.INSTANCE.draw(textRenderer, line, 4, y, 255);
            y += ENTRY_HEIGHT;
        }
    }

    /** Colours the visible slice of the input the way modern colours a command as you type it. */
    public TextDrawer.Line highlight(final String visible, final int firstCharacterIndex) {
        if (parse == null) {
            return new TextDrawer.Line(List.of(new Texts.Segment(visible, PLAIN_STYLE)));
        }

        final List<Texts.Segment> segments = new ArrayList<>();
        int consumed = 0;
        int styleIndex = -1;

        // Ranges are absolute over the whole input; the visible slice starts here.
        final int offset = firstCharacterIndex;

        final CommandContextBuilder<RetroCommandSource> context = parse.getContext().getLastChild();
        for (final ParsedArgument<RetroCommandSource, ?> argument : context.getArguments().values()) {
            styleIndex = (styleIndex + 1) % HIGHLIGHT_STYLES.length;

            final int start = Math.max(argument.getRange().getStart() - offset, 0);
            if (start >= visible.length()) {
                break;
            }

            final int end = Math.min(argument.getRange().getEnd() - offset, visible.length());
            if (end > 0) {
                segments.add(new Texts.Segment(visible.substring(consumed, start), PLAIN_STYLE));
                segments.add(new Texts.Segment(visible.substring(start, end), HIGHLIGHT_STYLES[styleIndex]));
                consumed = end;
            }
        }

        if (parse.getReader().canRead()) {
            final int errorStart = Math.max(parse.getReader().getCursor() - offset, 0);
            if (errorStart < visible.length()) {
                final int errorEnd = Math.min(errorStart + parse.getReader().getRemainingLength(), visible.length());
                segments.add(new Texts.Segment(visible.substring(consumed, errorStart), PLAIN_STYLE));
                segments.add(new Texts.Segment(visible.substring(errorStart, errorEnd), ERROR_STYLE));
                consumed = errorEnd;
            }
        }

        segments.add(new Texts.Segment(visible.substring(consumed), PLAIN_STYLE));
        return new TextDrawer.Line(segments);
    }

    /** The scrollable list of completions. */
    private final class Window {
        private final Suggestions suggestions;
        private final List<Suggestion> entries;
        private final int x;
        private final int width;

        private int selection;
        private int scrollOffset;

        private Window(final Suggestions suggestions) {
            this.suggestions = suggestions;
            this.entries = suggestions.getList();

            int widest = 0;
            for (final Suggestion suggestion : entries) {
                widest = Math.max(widest, textRenderer.getWidth(suggestion.getText()));
            }
            this.width = widest + 2;

            // Line the window up with the text it would replace, but keep it on screen.
            final String visible = input.visibleText();
            final int start = Math.min(suggestions.getRange().getStart(), input.getText().length());
            final int visibleStart = Math.max(0, Math.min(start - input.getFirstCharacterIndex(), visible.length()));
            final int offset = textRenderer.getWidth(visible.substring(0, visibleStart));
            this.x = Math.max(2, Math.min(4 + offset, owner.width - width - 2));

            // Modern previews the first entry as soon as the window appears.
            select(0);
        }

        private void select(final int index) {
            selection = Math.floorMod(index, entries.size());

            if (selection < scrollOffset) {
                scrollOffset = selection;
            } else if (selection >= scrollOffset + MAX_VISIBLE) {
                scrollOffset = selection - MAX_VISIBLE + 1;
            }

            // Modern previews the highlighted entry as ghost text after the cursor.
            final String applied = entries.get(selection).apply(input.getText());
            input.setSuggestion(applied.startsWith(input.getText()) ? applied.substring(input.getText().length()) : "");
        }

        private void scroll(final int amount) {
            final int max = Math.max(0, entries.size() - MAX_VISIBLE);
            scrollOffset = Math.max(0, Math.min(scrollOffset - amount, max));
        }

        private void complete() {
            completing = true;
            input.setText(entries.get(selection).apply(input.getText()));
            completing = false;
            window = null;
            refresh();
        }

        private boolean mouseClicked(final int mouseX, final int mouseY, final int button) {
            if (button != 0) {
                return false;
            }

            final int visibleCount = Math.min(entries.size(), MAX_VISIBLE);
            final int top = topY(visibleCount);

            if (mouseX < x || mouseX > x + width || mouseY < top || mouseY > top + visibleCount * ENTRY_HEIGHT) {
                return false;
            }

            select(scrollOffset + (mouseY - top) / ENTRY_HEIGHT);
            complete();
            return true;
        }

        private int topY(final int visibleCount) {
            return owner.height - 14 - visibleCount * ENTRY_HEIGHT;
        }

        private void render(final int mouseX, final int mouseY) {
            final int visibleCount = Math.min(entries.size(), MAX_VISIBLE);
            final int top = topY(visibleCount);

            fill(x, top, x + width, top + visibleCount * ENTRY_HEIGHT, BACKGROUND);

            for (int i = 0; i < visibleCount; i++) {
                final int index = scrollOffset + i;
                final Suggestion suggestion = entries.get(index);
                final int y = top + i * ENTRY_HEIGHT + 2;
                drawTextWithShadow(textRenderer, suggestion.getText(), x + 1, y, index == selection ? SELECTED_COLOR : UNSELECTED_COLOR);
            }

            if (entries.size() > MAX_VISIBLE) {
                renderScrollBar(top, visibleCount);
            }

            renderTooltip(top, visibleCount, mouseX, mouseY);
        }

        private void renderScrollBar(final int top, final int visibleCount) {
            final int trackHeight = visibleCount * ENTRY_HEIGHT;
            final int barHeight = Math.max(4, trackHeight * visibleCount / entries.size());
            final int travel = trackHeight - barHeight;
            final int barY = top + travel * scrollOffset / Math.max(1, entries.size() - visibleCount);

            fill(x + width, top, x + width + 2, top + trackHeight, 0x40FFFFFF);
            fill(x + width, barY, x + width + 2, barY + barHeight, 0xFFAAAAAA);
        }

        /** Shows the highlighted entry's tooltip, which is where argument descriptions surface. */
        private void renderTooltip(final int top, final int visibleCount, final int mouseX, final int mouseY) {
            final Suggestion suggestion = entries.get(selection);
            if (suggestion.getTooltip() == null) {
                return;
            }

            final Text tooltip = Texts.of(suggestion.getTooltip());
            final TextDrawer.Line line = TextDrawer.flatten(tooltip);
            final int tooltipWidth = TextDrawer.INSTANCE.width(textRenderer, line);
            final int tooltipX = Math.min(x, owner.width - tooltipWidth - 4);
            final int tooltipY = top - ENTRY_HEIGHT;

            fill(tooltipX, tooltipY - 1, tooltipX + tooltipWidth + 2, tooltipY + ENTRY_HEIGHT - 3, BACKGROUND);
            TextDrawer.INSTANCE.draw(textRenderer, line, tooltipX + 1, tooltipY, 255);
        }
    }
}
