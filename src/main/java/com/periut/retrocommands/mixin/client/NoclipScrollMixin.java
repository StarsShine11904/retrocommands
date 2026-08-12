package com.periut.retrocommands.mixin.client;

import com.periut.retrocommands.command.builtin.NoclipCommand;
import com.periut.retrocommands.mixin.access.InGameHudAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes the scroll wheel a throttle while noclip is on.
 *
 * <p>A {@link Redirect} on the hotbar scroll rather than an injection into the mouse loop: it takes over
 * exactly the one thing the wheel does in beta and leaves the rest of that loop - the discrete-scroll option,
 * the button events - untouched. Scrolling still changes the held item as soon as flight is off.
 *
 * <p>The readout goes to the HUD's transient overlay line rather than to chat, because a wheel produces a
 * dozen events in a spin and a dozen chat lines would be worse than no readout at all.
 */
@Mixin(Minecraft.class)
public abstract class NoclipScrollMixin {

    @Shadow public net.minecraft.entity.player.ClientPlayerEntity player;
    @Shadow public net.minecraft.client.gui.hud.InGameHud inGameHud;

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerInventory;scrollInHotbar(I)V"
            ),
            require = 0
    )
    private void retrocommands$scrollSpeed(PlayerInventory inventory, int amount) {
        if (this.player == null || !NoclipCommand.isActive(this.player.name)) {
            inventory.scrollInHotbar(amount);
            return;
        }

        // Scroll up is a positive wheel delta and means faster, which is the way round every other
        // scroll-as-throttle works.
        double factor = amount > 0 ? NoclipCommand.SPEED_STEP : 1.0 / NoclipCommand.SPEED_STEP;
        double speed = NoclipCommand.changeSpeed(this.player.name, factor);
        String message = speed < 0.0
                ? "Noclip speed " + NoclipCommand.format(NoclipCommand.speed(this.player.name)) + "x (limit)"
                : "Noclip speed " + NoclipCommand.format(speed) + "x";

        if (this.inGameHud != null) {
            InGameHudAccessor overlay = (InGameHudAccessor) this.inGameHud;
            overlay.spc$setOverlayMessage(message);
            overlay.spc$setOverlayRemaining(40);
            overlay.spc$setOverlayTinted(false);
        }
    }
}
