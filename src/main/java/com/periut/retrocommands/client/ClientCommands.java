package com.periut.retrocommands.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import com.periut.retrocommands.RetroCommands;
import com.periut.retrocommands.command.RegistrationEnvironment;
import com.periut.retrocommands.command.RetroCommandManager;
import com.periut.retrocommands.command.RetroCommandSource;
import com.periut.retrocommands.network.ClientSuggestions;
import com.periut.retrocommands.text.Text;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.util.concurrent.CompletableFuture;

/**
 * The client's view of the command system.
 *
 * <p>Which dispatcher answers depends on where the player is. In singleplayer the client owns the
 * only dispatcher there is and runs commands against its own world - beta has no integrated server
 * to hand them to. On a server running this mod, the tree the server sent is used for parsing,
 * colouring and completions, and the command itself is sent on to be executed there. On a vanilla
 * server the local tree is used, which at least colours the syntax of the commands beta understands.
 */
public final class ClientCommands {
    private static RetroCommandManager local;
    private static RetroCommandSource source;
    private static CommandDispatcher<RetroCommandSource> serverDispatcher;

    private ClientCommands() {
    }

    /** Builds the client tree; called when a world is loaded, because commands act on that world. */
    public static void onWorldLoad() {
        local = new RetroCommandManager(RegistrationEnvironment.INTEGRATED);
        RetroCommandManager.setInstance(local);
        source = null;
    }

    public static void onDisconnect() {
        serverDispatcher = null;
        source = null;
    }

    /** Replaces the parsing tree with the one a server sent. */
    public static void setServerDispatcher(final CommandDispatcher<RetroCommandSource> dispatcher) {
        serverDispatcher = dispatcher;
    }

    public static CommandDispatcher<RetroCommandSource> getDispatcher() {
        if (serverDispatcher != null) {
            return serverDispatcher;
        }
        ensureLocal();
        return local.getDispatcher();
    }

    /**
     * The source used for parsing and completions.
     *
     * <p>Rebuilt on demand rather than cached, because it carries the player's position and a
     * selector like {@code @e[distance=..5]} has to be judged against where they are now.
     */
    public static RetroCommandSource getSource() {
        final Minecraft minecraft = minecraft();
        if (minecraft == null) {
            return source;
        }
        source = ClientCommandSources.create(minecraft);
        return source;
    }

    public static ParseResults<RetroCommandSource> parse(final StringReader reader) {
        return getDispatcher().parse(reader, getSource());
    }

    /**
     * @param cursor the cursor position within the command, excluding its leading slash
     */
    public static CompletableFuture<Suggestions> suggest(final ParseResults<RetroCommandSource> parse, final int cursor) {
        // A server that runs this mod knows things the client cannot - who is online, what a mod
        // added - so ask it, and fall back to the local tree while the answer is in flight.
        if (RetroCommands.mp_rc && isRemote()) {
            return ClientSuggestions.request(parse.getReader().getString(), cursor);
        }
        return getDispatcher().getCompletionSuggestions(parse, cursor);
    }

    /** Runs a command locally. Only correct in singleplayer; a server executes its own. */
    public static void execute(final String command) {
        ensureLocal();
        local.execute(getSource(), command);
    }

    public static boolean isRemote() {
        final Minecraft minecraft = minecraft();
        return minecraft != null && minecraft.world != null && minecraft.world.isRemote;
    }

    public static Text describeUnavailable() {
        return Text.literal("Commands are not available yet");
    }

    private static void ensureLocal() {
        if (local == null) {
            onWorldLoad();
        }
    }

    private static Minecraft minecraft() {
        return (Minecraft) FabricLoader.getInstance().getGameInstance();
    }
}
