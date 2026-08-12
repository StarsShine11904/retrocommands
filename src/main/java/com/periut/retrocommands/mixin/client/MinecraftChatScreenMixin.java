package com.periut.retrocommands.mixin.client;

import com.periut.retrocommands.client.ClientCommands;
import com.periut.retrocommands.client.gui.RetroChatHud;
import com.periut.retrocommands.client.gui.RetroChatScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Opens the mod's chat screen wherever the game would have opened beta's.
 *
 * <p>Substituting the screen on its way into {@code setScreen} rather than patching the key handler
 * means every route into chat ends up here - the chat key, and any other mod that opens it.
 * {@code SleepingChatScreen} is deliberately left alone: it is a different screen with a bed
 * attached, and an exact class check is what keeps it working.
 */
@Mixin(Minecraft.class)
public class MinecraftChatScreenMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen retrocommands$useOwnChatScreen(final Screen screen) {
        return screen != null && screen.getClass() == ChatScreen.class ? new RetroChatScreen() : screen;
    }

    /**
     * The command tree describes the world it acts on, so it is built once that world exists and
     * discarded with it.
     */
    @Inject(method = "setWorld(Lnet/minecraft/world/World;Ljava/lang/String;)V", at = @At("TAIL"), require = 0)
    private void retrocommands$onWorldChanged(final World world, final String message, final CallbackInfo ci) {
        if (world == null) {
            ClientCommands.onDisconnect();
        } else {
            ClientCommands.onWorldLoad();
            RetroChatHud.getInstance().clear();
        }
    }
}
