package com.periut.retrocommands.client.gui;

import net.minecraft.client.gui.screen.Screen;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;

/**
 * Clipboard access.
 *
 * <p>Beta can only read the clipboard - {@link Screen#getClipboard()} wraps LWJGL 2's read-only
 * {@code Sys.getClipboard()} - so copying goes through AWT instead. Every call is guarded: a
 * headless JVM or a desktop that refuses clipboard access must cost the player a copy, not the
 * chat screen.
 */
public final class Clipboard {
    private Clipboard() {
    }

    public static String read() {
        try {
            final String contents = Screen.getClipboard();
            return contents == null ? "" : contents;
        } catch (final RuntimeException | LinkageError ignored) {
            return "";
        }
    }

    public static void write(final String contents) {
        if (contents == null || contents.isEmpty()) {
            return;
        }
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(contents), null);
        } catch (final RuntimeException | LinkageError ignored) {
            // Nothing to be done about it, and nothing worth interrupting the player over.
        }
    }

    /** Only used to tell whether AWT is usable at all, for the copy-on-click chat action. */
    public static boolean isAvailable() {
        try {
            return Toolkit.getDefaultToolkit().getSystemClipboard().isDataFlavorAvailable(DataFlavor.stringFlavor) || true;
        } catch (final RuntimeException | LinkageError ignored) {
            return false;
        }
    }
}
