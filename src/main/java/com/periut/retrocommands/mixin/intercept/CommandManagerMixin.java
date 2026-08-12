package com.periut.retrocommands.mixin.intercept;

import com.periut.retrocommands.command.RetroCommandManager;
import com.periut.retrocommands.command.ServerCommandSources;
import net.minecraft.server.command.Command;
import net.minecraft.server.command.ServerCommandHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Routes console commands through the dispatcher instead of beta's hand-written parser.
 *
 * <p>Vanilla's handler is replaced outright rather than fallen back to: every command it knows has
 * an equivalent here, and leaving two parsers in play would mean two different sets of error
 * messages for the same typo.
 */
@Mixin(ServerCommandHandler.class)
public class CommandManagerMixin {
    @Inject(method = "executeCommand", at = @At("HEAD"), cancellable = true)
    private void retrocommands$dispatch(Command command, CallbackInfo ci) {
        final RetroCommandManager manager = RetroCommandManager.getInstance();
        if (manager == null) {
            return;
        }

        manager.execute(ServerCommandSources.forConsole(command.output), command.commandAndArgs);
        ci.cancel();
    }
}
