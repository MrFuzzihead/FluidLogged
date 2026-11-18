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

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;
import net.minecraftforge.common.util.ForgeDirection;

/** Faces of vanilla holder shapes that stop fluid, following the modern waterlogging rules. */
public final class FaceSolidity {

    public enum Shape {
        STAIRS,
        SLAB,
        LEAVES,
        TRAPDOOR,
        OTHER
    }

    // Sides covered by an open 1.7.10 trapdoor, indexed by meta & 3 (BlockTrapDoor.func_150117_b)
    private static final ForgeDirection[] OPEN_TRAPDOOR_SIDE = { ForgeDirection.SOUTH, ForgeDirection.NORTH,
        ForgeDirection.EAST, ForgeDirection.WEST };

    private FaceSolidity() {}

    public static Shape shapeOf(Block block) {
        if (block instanceof BlockStairs) return Shape.STAIRS;
        if (block instanceof BlockSlab) return Shape.SLAB;
        if (block instanceof BlockLeaves) return Shape.LEAVES;
        if (block instanceof BlockTrapDoor) return Shape.TRAPDOOR;
        return Shape.OTHER;
    }

    public static boolean isSolid(Shape shape, int meta, ForgeDirection side) {
        switch (shape) {
            case STAIRS: {
                boolean upsideDown = (meta & 4) != 0;
                if (side == ForgeDirection.UP) {
                    return upsideDown;
                }
                if (side == ForgeDirection.DOWN) {
                    return !upsideDown;
                }
                // Same back-face rule as Forge Block.isSideSolid for stairs: meta & 3 plus side ordinal is 5.
                return (meta & 3) + side.ordinal() == 5;
            }
            case SLAB: {
                boolean top = (meta & 8) != 0;
                return top ? side == ForgeDirection.UP : side == ForgeDirection.DOWN;
            }
            case LEAVES:
                // Modern leaves keep a full-cube collision shape, so fluid passes no face.
                return true;
            case TRAPDOOR: {
                if ((meta & 4) != 0) {
                    return side == OPEN_TRAPDOOR_SIDE[meta & 3];
                }
                boolean top = (meta & 8) != 0;
                return top ? side == ForgeDirection.UP : side == ForgeDirection.DOWN;
            }
            default:
                return false;
        }
    }
}
