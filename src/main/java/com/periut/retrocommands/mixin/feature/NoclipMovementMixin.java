package com.periut.retrocommands.mixin.feature;

import com.periut.retrocommands.command.builtin.NoclipCommand;
import com.periut.retrocommands.mixin.access.EntityAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The flight itself: replaces a noclipping player's movement outright.
 *
 * <p>{@code LivingEntity.travel} is the right place and the only tidy one. It is where beta turns movement
 * input into velocity and calls {@code move}, and it already receives the two input axes as arguments, so
 * cancelling it and doing the movement here takes over exactly the physics that should be taken over -
 * gravity, friction, water drag, ladders, slipperiness - and nothing else. Everything upstream of it (input
 * collection, portals, the sneak camera offset) still runs.
 *
 * <p>Placed on {@code LivingEntity} rather than the client player class because {@code travel} is declared
 * here, and because the check has to hold on both sides: a server that does not know the player is noclipping
 * simulates the same move with collision and shoves them back out of the wall.
 */
@Mixin(LivingEntity.class)
public abstract class NoclipMovementMixin {

    @Shadow protected float sidewaysSpeed;
    @Shadow protected float forwardSpeed;
    @Shadow protected boolean jumping;

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void retrocommands$noclipFlight(float sideways, float forward, CallbackInfo ci) {
        if (!((Object) this instanceof PlayerEntity player) || !NoclipCommand.isActive(player.name)) {
            return;
        }

        Entity self = (Entity) (Object) this;
        // Set every tick rather than once at toggle time: a respawn or a dimension change builds a new entity,
        // and this way the flag on the command is the single source of truth for whether flight is on.
        self.noClip = true;
        self.onGround = false;
        ((EntityAccessor) self).spc$setFallDistance(0.0F);

        double speed = NoclipCommand.BASE_SPEED * NoclipCommand.speed(player.name);

        // Beta's own input-to-direction conversion, from Entity.moveNonSolid: normalise the input pair, then
        // rotate it by yaw. Copied rather than reused because moveNonSolid ADDS to velocity, and the whole
        // point here is that velocity does not accumulate.
        double dx = 0.0;
        double dz = 0.0;
        float magnitude = MathHelper.sqrt(sideways * sideways + forward * forward);
        if (magnitude >= 0.01F) {
            float scale = (float) (speed / Math.max(1.0F, magnitude));
            float scaledSideways = sideways * scale;
            float scaledForward = forward * scale;
            float sin = MathHelper.sin(self.yaw * (float) Math.PI / 180.0F);
            float cos = MathHelper.cos(self.yaw * (float) Math.PI / 180.0F);
            dx = scaledSideways * cos - scaledForward * sin;
            dz = scaledForward * cos + scaledSideways * sin;
        }

        double dy = 0.0;
        if (this.jumping) {
            dy = speed;
        } else if (self.isSneaking()) {
            dy = -speed;
        }

        self.move(dx, dy, dz);

        // No leftover velocity at all. Anything left here would be re-applied next tick by the code that
        // normally decays it, and with gravity skipped it would only ever grow.
        self.velocityX = 0.0;
        self.velocityY = 0.0;
        self.velocityZ = 0.0;
        ci.cancel();
    }
}
