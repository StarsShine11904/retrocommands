package com.periut.retrocommands.mixin.intercept;

import com.periut.retrocommands.command.RetroCommandManager;
import com.periut.retrocommands.command.ServerCommandSources;
import com.periut.retrocommands.command.builtin.NoclipCommand;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayerPacketHandlerMixin {
    @Shadow private ServerPlayerEntity player;

    @Shadow public abstract void sendPacket(Packet arg);

    /**
     * Stops the server from dragging a noclipping player back out of the wall they just flew into.
     *
     * <p>{@code onPlayerMove} re-simulates the client's move and, if the player ends up somewhere that
     * collides when they did not start there, teleports them back. Making the server player {@code noClip}
     * (which the flight mixin does every tick, on both sides) is not enough on its own: the collision test
     * after the move is a plain block-box query and does not consult {@code noClip}. This is the one call that
     * has to be suppressed, and only for a player who is actually flying - every other rubber-band correction
     * the server does still works.
     */
    @Redirect(
            method = "onPlayerMove",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;teleport(DDDFF)V"
            ),
            require = 0
    )
    private void retrocommands$allowNoclipMovement(ServerPlayNetworkHandler handler,
            double x, double y, double z, float yaw, float pitch) {
        if (NoclipCommand.isActive(this.player.name)) {
            return;
        }
        handler.teleport(x, y, z, yaw, pitch);
    }

    /**
     * Every command a player types goes to the dispatcher, whether or not they are an operator -
     * the tree's own requirements decide what they may run, and an unknown command has to produce
     * the same "Unknown command" it would in modern rather than beta's silent shrug.
     */
    @Inject(method = "handleCommand", at = @At(value = "HEAD"), cancellable = true)
    private void retrocommands$dispatch(String command, CallbackInfo ci) {
        final RetroCommandManager manager = RetroCommandManager.getInstance();
        if (manager == null) {
            return;
        }

        manager.execute(ServerCommandSources.forPlayer(this.player), command.substring(1));
        ci.cancel();
    }
}
