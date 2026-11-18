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

package mega.fluidlogged.internal.flow;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mega.fluidlogged.FLConstants;
import mega.fluidlogged.internal.mixin.mixins.common.accessor.AccessorBlockDynamicLiquid;
import mega.fluidlogged.internal.mixin.mixins.common.accessor.AccessorBlockLiquid;

/**
 * Flow of a fluid stored inside a block. A stored fluid acts as a permanent source behind the block's closed
 * faces; this replaces the previous practice of re-running vanilla {@code BlockDynamicLiquid.updateTick} against
 * the host block and relying on redirects to make that unsafe simulation see the stored fluid.
 *
 * <p>
 * Vanilla water and lava are handled here. A Forge {@link BlockFluidClassic} keeps its own tick logic (with the
 * existing compatibility redirects); other fluids do not flow out of blocks.
 * </p>
 */
public final class FLFlow {

    private static final ForgeDirection[] SIDEWAYS = { ForgeDirection.WEST, ForgeDirection.EAST, ForgeDirection.NORTH,
        ForgeDirection.SOUTH };
    private static final Logger LOG = LogManager.getLogger(FLConstants.MOD_NAME);
    private static final Set<String> WARNED = Collections.synchronizedSet(new HashSet<>());

    private FLFlow() {}

    /** Spreads the stored fluid at (x, y, z) once, like a vanilla source block would. */
    public static void tick(World world, int x, int y, int z, Fluid fluid) {
        Block flowing = flowingBlock(fluid);
        if (flowing == null) {
            Block block = fluid.getBlock();
            if (block instanceof BlockFluidClassic) {
                block.updateTick(world, x, y, z, world.rand);
            } else if (WARNED.add(fluid.getName())) {
                LOG.warn("Stored fluid '{}' is not a vanilla liquid; it does not flow out of blocks", fluid.getName());
            }
            return;
        }

        AccessorBlockDynamicLiquid liquid = (AccessorBlockDynamicLiquid) flowing;
        if (canPass(world, x, y, z, ForgeDirection.DOWN) && liquid.fl$canFlowInto(world, x, y - 1, z)) {
            // Vanilla BlockDynamicLiquid.updateTick: lava falling onto water turns it into stone.
            if (flowing.getMaterial() == Material.lava && world.getBlock(x, y - 1, z)
                .getMaterial() == Material.water) {
                world.setBlock(x, y - 1, z, Blocks.stone);
                ((AccessorBlockLiquid) flowing).fl$fizz(world, x, y - 1, z);
                return;
            }
            liquid.fl$flowInto(world, x, y - 1, z, 8);
            return;
        }

        int level = flowing.getMaterial() == Material.lava && !world.provider.isHellWorld ? 2 : 1;
        // Index order of vanilla func_149808_o: x-1, x+1, z-1, z+1.
        boolean[] dirs = liquid.fl$optimalFlowDirections(world, x, y, z);
        for (int i = 0; i < SIDEWAYS.length; i++) {
            ForgeDirection direction = SIDEWAYS[i];
            if (dirs[i] && canPass(world, x, y, z, direction)) {
                liquid.fl$flowInto(world, x + direction.offsetX, y, z + direction.offsetZ, level);
            }
        }
    }

    /** Returns the flowing vanilla block for water and lava, or null for every other fluid. */
    public static Block flowingBlock(Fluid fluid) {
        if (fluid == FluidRegistry.WATER) {
            return Blocks.flowing_water;
        }
        if (fluid == FluidRegistry.LAVA) {
            return Blocks.flowing_lava;
        }
        return null;
    }

    /** Returns false if the face between this cell and its neighbour in {@code direction} is closed. */
    public static boolean canPass(IBlockAccess world, int x, int y, int z, ForgeDirection direction) {
        return !isSideClosed(world, x, y, z, direction) && !isSideClosed(
            world,
            x + direction.offsetX,
            y + direction.offsetY,
            z + direction.offsetZ,
            direction.getOpposite());
    }

    private static boolean isSideClosed(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        Block block = world.getBlock(x, y, z);
        FaceSolidity.Shape shape = FaceSolidity.shapeOf(block);
        if (shape != FaceSolidity.Shape.OTHER) {
            return FaceSolidity.isSolid(shape, world.getBlockMetadata(x, y, z), side);
        }
        return block.isSideSolid(world, x, y, z, side);
    }
}
