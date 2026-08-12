package com.periut.retrocommands;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.ornithemc.osl.networking.api.ChannelIdentifiers;
import net.ornithemc.osl.networking.api.ChannelRegistry;

public class RetroCommandsNetworking {
	public static final NamespacedIdentifier OP_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retrocommands", "op"), true, false);
	public static final NamespacedIdentifier PLAYERS_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retrocommands", "players"), true, false);
	public static final NamespacedIdentifier DISABLED_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retrocommands", "disabled"), true, false);

	/** The server's command tree, sent once a player has joined and again whenever their rights change. */
	public static final NamespacedIdentifier COMMANDS_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retrocommands", "commands"), true, false);
	/** Completion requests from a client, and the answers to them - keyed by a request id. */
	public static final NamespacedIdentifier SUGGEST_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retrocommands", "suggest"), true, true);
	/** Command output as a component, for clients that can render one. */
	public static final NamespacedIdentifier MESSAGE_CHANNEL =
		ChannelRegistry.register(ChannelIdentifiers.from("retrocommands", "message"), true, false);
}
