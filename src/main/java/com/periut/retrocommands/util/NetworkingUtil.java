package com.periut.retrocommands.util;

import com.periut.retrocommands.RetroCommands;
import com.periut.retrocommands.RetroCommandsNetworking;
import com.periut.retrocommands.client.ClientCommands;
import com.periut.retrocommands.client.gui.RetroChatHud;
import com.periut.retrocommands.network.ClientSuggestions;
import com.periut.retrocommands.network.CommandTreeSerializer;
import com.periut.retrocommands.text.TextCodec;
import com.periut.retrocommands.text.Translations;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.resource.language.I18n;
import net.ornithemc.osl.networking.api.client.ClientPlayNetworking;

import java.util.List;

public class NetworkingUtil implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // The game's translation table exists only on a client, and only this side may touch the
        // class that holds it. Beta echoes the key back for an unknown one, so map that to null -
        // Translations treats null as "ask the next source".
        Translations.setResolver(key -> {
            final String translated = I18n.getTranslation(key);
            return translated == null || translated.equals(key) ? null : translated;
        });

        ClientPlayNetworking.registerListener(RetroCommandsNetworking.OP_CHANNEL, (ctx, buffer) -> {
            ctx.ensureOnMainThread();
            RetroCommands.mp_op = buffer.readBoolean();
            RetroCommands.mp_rc = true;
        });

        ClientPlayNetworking.registerListener(RetroCommandsNetworking.PLAYERS_CHANNEL, (ctx, buffer) -> {
            ctx.ensureOnMainThread();
            RetroCommands.player_names = buffer.readString().split(",");
        });

        ClientPlayNetworking.registerListener(RetroCommandsNetworking.DISABLED_CHANNEL, (ctx, buffer) -> {
            ctx.ensureOnMainThread();
            RetroCommands.disabled_commands = List.of(buffer.readString().split(","));
        });

        // The server's command tree: what this player may run, described well enough to parse,
        // colour and complete locally.
        ClientPlayNetworking.registerListener(RetroCommandsNetworking.COMMANDS_CHANNEL, (ctx, buffer) -> {
            ctx.ensureOnMainThread();
            ClientCommands.setServerDispatcher(CommandTreeSerializer.read(buffer));
            RetroCommands.mp_rc = true;
        });

        ClientPlayNetworking.registerListener(RetroCommandsNetworking.SUGGEST_CHANNEL, (ctx, buffer) -> {
            ctx.ensureOnMainThread();
            ClientSuggestions.onResponse(buffer);
        });

        // Command output as a component, so hover text and click actions survive the trip.
        ClientPlayNetworking.registerListener(RetroCommandsNetworking.MESSAGE_CHANNEL, (ctx, buffer) -> {
            ctx.ensureOnMainThread();
            RetroChatHud.getInstance().addMessage(TextCodec.fromJson(buffer.readString()));
        });
    }
}
