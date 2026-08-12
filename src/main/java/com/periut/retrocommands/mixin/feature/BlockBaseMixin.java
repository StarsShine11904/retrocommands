package com.periut.retrocommands.mixin.feature;

import com.periut.retrocommands.api.ItemInstanceStr;
import com.periut.retrocommands.optionaldep.stapi.block.SpecialMobSpawnerBlock;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityRegistry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Block.class)
public class BlockBaseMixin {
    /** Beta's monster spawner. Hardcoded because {@code Block.SPAWNER} is not loaded yet here. */
    private static final int SPAWNER_ID = 52;

    @Redirect(method = "<clinit>", at = @At(value = "NEW", target = "(II)Lnet/minecraft/block/SpawnerBlock;"))
    private static SpawnerBlock staticBlock(int i, int j) {
        if (FabricLoader.getInstance().isModLoaded("station-blockitems-v0"))
            return new SpecialMobSpawnerBlock(i, j);
        else
            return new SpawnerBlock(i, j);
    }

    /**
     * Carries the mob a spawner was given with into the block that gets placed.
     *
     * <p>The block is identified by its id rather than by its name: reading the name goes through
     * the client-only translation table, which is not merely wasteful on a dedicated server but
     * throws {@link NoClassDefFoundError} there.
     */
    @Inject(method = "onPlaced(Lnet/minecraft/world/World;IIII)V", at = @At("HEAD"))
    public void onBlockPlaced(World i, int j, int k, int l, int direction, CallbackInfo ci) {
        if (i.getBlockId(j, k, l) == SPAWNER_ID) {
            try {
                Box b = Box.create(j - 5, k - 5, l - 5, j + 5, k + 5, l + 5);
                List<PlayerEntity> players = i.collectEntitiesByClass(PlayerEntity.class, b);
                String mob = null;
                if (players.size() == 1) {
                    if (players.get(0).inventory.getSelectedItem() != null)
                        mob = ((ItemInstanceStr) (Object) players.get(0).inventory.getSelectedItem()).spc$getStr();
                } else {
                    for (PlayerEntity p : players) {
                        ItemStack held = p.inventory.getSelectedItem();
                        if (held != null && held.itemId == SPAWNER_ID) {
                            mob = ((ItemInstanceStr) (Object) held).spc$getStr();
                            break;
                        }
                    }
                }
                if (mob == null)
                    mob = "Pig";

                Entity entity = EntityRegistry.create(mob, i);
                if (entity instanceof LivingEntity) {
                    ((MobSpawnerBlockEntity) i.getBlockEntity(j, k, l)).setSpawnedEntityId(entity.getRegistryEntry());
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
