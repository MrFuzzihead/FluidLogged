/*
 * This file is part of FluidLogged.
 * Copyright (C) 2025 The MEGA Team, FalsePattern
 * All Rights Reserved
 * The above copyright notice, this permission notice and the word "MEGA"
 * shall be included in all copies or substantial portions of the Software.
 * FluidLogged is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * FluidLogged is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with FluidLogged. If not, see <https://www.gnu.org/licenses/>.
 */

package mega.fluidlogged.internal;

import java.util.Random;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.IFluidBlock;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import cpw.mods.fml.common.eventhandler.Event;
import lombok.val;
import mega.fluidlogged.api.FLBlockAccess;
import mega.fluidlogged.internal.flow.FLFlow;
import mega.fluidlogged.internal.mixin.hook.FLWorld;

public class FLUtil {

    public static Block getFluidOrBlock(IBlockAccess access, int x, int y, int z) {
        if (!(access instanceof FLBlockAccess)) return access.getBlock(x, y, z);
        val fluid = ((FLBlockAccess) access).fl$getFluid(x, y, z);
        if (fluid == null) return access.getBlock(x, y, z);
        val block = fluid.getBlock();
        if (block == null) return access.getBlock(x, y, z);
        return block;
    }

    public static int getFluidMeta(IBlockAccess access, int x, int y, int z, int max) {
        if (!(access instanceof FLBlockAccess)) return access.getBlockMetadata(x, y, z);
        val fluid = ((FLBlockAccess) access).fl$getFluid(x, y, z);
        if (fluid == null) return access.getBlockMetadata(x, y, z);
        val block = fluid.getBlock();
        if (block == null) return access.getBlockMetadata(x, y, z);
        return max;
    }

    /**
     * Light emitted by a fluidlogged fluid. Uses the fluid's registered luminosity and the fluid block's static light
     * value rather than {@link Block#getLightValue(IBlockAccess, int, int, int)}, which can recurse back into the
     * host block.
     */
    public static int fluidLightValue(@Nullable Fluid fluid) {
        if (fluid == null) return 0;
        int light = fluid.getLuminosity();
        val block = fluid.getBlock();
        if (block != null) {
            light = Math.max(light, block.getLightValue());
        }
        return Math.min(15, Math.max(0, light));
    }

    /**
     * Light opacity of a fluidlogged fluid, taken from its block's static opacity. Used for the chunk height map and
     * skylight calculations, mirroring how vanilla water dims light while a waterloggable host block does not.
     */
    public static int fluidLightOpacity(@Nullable Fluid fluid) {
        if (fluid == null) return 0;
        val block = fluid.getBlock();
        if (block == null) return 0;
        return Math.min(255, Math.max(0, block.getLightOpacity()));
    }

    /** Relights a cell after its stored fluid changed, covering both emission and opacity. */
    public static void updateFluidLight(World world, int x, int y, int z, @Nullable Fluid oldFluid,
        @Nullable Fluid newFluid) {
        val lightChanged = fluidLightValue(oldFluid) != fluidLightValue(newFluid);
        val opacityChanged = fluidLightOpacity(oldFluid) != fluidLightOpacity(newFluid);
        if (lightChanged || opacityChanged) {
            world.updateLightByType(EnumSkyBlock.Block, x, y, z);
        }
        if (opacityChanged) {
            world.updateLightByType(EnumSkyBlock.Sky, x, y, z);
        }
    }

    public static void fireBucketEvent(ItemStack item, World world, EntityPlayer player,
        Consumer<ItemStack> resultCallback, MovingObjectPosition pos) {
        val event = new FillBucketEvent(player, item, world, pos);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            resultCallback.accept(item);
            return;
        }

        if (event.getResult() != Event.Result.ALLOW) {
            return;
        }

        if (player.capabilities.isCreativeMode) {
            resultCallback.accept(item);
            return;
        }

        if (--item.stackSize <= 0) {
            resultCallback.accept(event.result);
            return;
        }

        if (!player.inventory.addItemStackToInventory(event.result)) {
            player.dropPlayerItemWithRandomChoice(event.result, false);
        }

        resultCallback.accept(item);
    }

    public static void onFluidPlacedInto(@NotNull World world, int x, int y, int z, @NotNull Block block,
        @NotNull Block fluidBlock) {
        if (fluidBlock instanceof BlockLiquid) {
            ((FLWorld) world).fl$scheduleFluidUpdate(x, y, z, block, fluidBlock.tickRate(world));
        } else if (fluidBlock instanceof IFluidBlock) {
            ((FLWorld) world).fl$scheduleFluidUpdate(x, y, z, block, fluidBlock.tickRate(world));
        }
    }

    public static void simulate(@NotNull World world, int x, int y, int z, @NotNull Random random, Fluid fluid) {
        FLFlow.tick(world, x, y, z, fluid);
    }

    public static @Nullable Fluid fromChunkBlock(@NotNull Chunk chunk, int x, int y, int z, @NotNull Block block) {
        val meta = chunk.getBlockMetadata(x, y, z);
        if (meta != 0) {
            return null;
        }
        return fromBucketBlock(block);
    }

    public static @Nullable Fluid fromBucketBlock(@NotNull Block block) {
        if (block == Blocks.flowing_water) {
            block = Blocks.water;
        } else if (block == Blocks.flowing_lava) {
            block = Blocks.lava;
        }
        return FluidRegistry.lookupFluidForBlock(block);
    }
}
