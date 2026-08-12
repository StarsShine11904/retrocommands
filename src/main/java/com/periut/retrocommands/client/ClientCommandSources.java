package com.periut.retrocommands.client;

import com.periut.retrocommands.RetroCommands;
import com.periut.retrocommands.client.gui.RetroChatHud;
import com.periut.retrocommands.command.Position;
import com.periut.retrocommands.command.RetroCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ClientPlayerEntity;

/**
 * Builds the command source a client runs commands as.
 *
 * <p>In singleplayer that is the whole story - beta has no integrated server, so the client is the
 * authority and holds every permission. Connected to a server, this source exists only to parse and
 * suggest; the server builds its own when the command actually arrives.
 */
public final class ClientCommandSources {
    private ClientCommandSources() {
    }

    public static RetroCommandSource create(final Minecraft minecraft) {
        final ClientPlayerEntity player = minecraft.player;
        final boolean remote = minecraft.world != null && minecraft.world.isRemote;

        // On a server the client only mirrors what the server told it about our rights.
        final int level = !remote || RetroCommands.mp_op
            ? RetroCommandSource.LEVEL_OWNER
            : RetroCommandSource.LEVEL_ALL;

        return new RetroCommandSource(
            message -> RetroChatHud.getInstance().addMessage(message),
            player,
            minecraft.world,
            player == null ? Position.ORIGIN : new Position(player.x, player.y, player.z),
            player == null ? 0.0f : player.yaw,
            player == null ? 0.0f : player.pitch,
            player == null ? "client" : player.name,
            level,
            null,
            true);
    }
}
