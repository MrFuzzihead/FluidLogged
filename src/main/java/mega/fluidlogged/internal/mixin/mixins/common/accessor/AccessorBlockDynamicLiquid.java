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

package mega.fluidlogged.internal.mixin.mixins.common.accessor;

import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Invokers for the private spreading helpers of {@code BlockDynamicLiquid}. */
@Mixin(BlockDynamicLiquid.class)
public interface AccessorBlockDynamicLiquid {

    @Invoker("func_149808_o")
    boolean[] fl$optimalFlowDirections(World world, int x, int y, int z);

    @Invoker("func_149813_h")
    void fl$flowInto(World world, int x, int y, int z, int meta);

    @Invoker("func_149809_q")
    boolean fl$canFlowInto(World world, int x, int y, int z);
}
