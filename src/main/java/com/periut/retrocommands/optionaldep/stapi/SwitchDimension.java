package com.periut.retrocommands.optionaldep.stapi;

import com.periut.retrocommands.dimension.BareTravelAgent;
import net.minecraft.entity.player.PlayerEntity;
import net.modificationstation.stationapi.api.registry.DimensionRegistry;
import net.modificationstation.stationapi.api.util.Identifier;
import net.modificationstation.stationapi.api.world.dimension.DimensionHelper;

/**
 * StationAPI's dimension transfer.
 *
 * <p>Every StationAPI type stays inside this class, which is only reached once
 * {@code station-dimensions-v0} is known to be loaded.
 */
public class SwitchDimension {
    /** @return false if no such dimension is registered */
    public static boolean go(final PlayerEntity player, final String id) {
        final Identifier dimension = Identifier.tryParse(id);
        if (dimension == null || DimensionRegistry.INSTANCE.get(dimension) == null) {
            return false;
        }
        DimensionHelper.switchDimension(player, dimension, 1, new BareTravelAgent());
        return true;
    }

    /** Moves a player to whichever dimension another entity is in. */
    public static boolean toDimensionOf(final PlayerEntity player, final int dimensionId) {
        return DimensionRegistry.INSTANCE.getId(dimensionId)
            .map(id -> {
                DimensionHelper.switchDimension(player, id, 1, new BareTravelAgent());
                return true;
            })
            .orElse(false);
    }
}
