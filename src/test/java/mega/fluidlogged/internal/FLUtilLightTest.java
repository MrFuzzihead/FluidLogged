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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.minecraft.block.Block;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;

import org.junit.jupiter.api.Test;

class FLUtilLightTest {

    @Test
    void updatesBlockAndSkyLightWhenOpacityChanges() {
        World world = mock(World.class);
        Block block = mock(Block.class);
        when(block.getLightOpacity()).thenReturn(3);
        Fluid water = new Fluid("fluidlogged_light_opacity").setBlock(block);

        FLUtil.updateFluidLight(world, 1, 2, 3, null, water);

        verify(world).updateLightByType(EnumSkyBlock.Block, 1, 2, 3);
        verify(world).updateLightByType(EnumSkyBlock.Sky, 1, 2, 3);
    }

    @Test
    void updatesOnlyBlockLightWhenEmissionChanges() {
        World world = mock(World.class);
        Block block = mock(Block.class);
        when(block.getLightOpacity()).thenReturn(0);
        when(block.getLightValue()).thenReturn(10);
        Fluid lava = new Fluid("fluidlogged_light_emission").setBlock(block);

        FLUtil.updateFluidLight(world, 1, 2, 3, null, lava);

        verify(world).updateLightByType(EnumSkyBlock.Block, 1, 2, 3);
        verify(world, never()).updateLightByType(EnumSkyBlock.Sky, 1, 2, 3);
    }

    @Test
    void doesNothingWhenLightAndOpacityAreUnchanged() {
        World world = mock(World.class);
        Block block = mock(Block.class);
        when(block.getLightOpacity()).thenReturn(0);
        Fluid fluid = new Fluid("fluidlogged_light_same").setBlock(block);

        FLUtil.updateFluidLight(world, 1, 2, 3, fluid, fluid);

        verify(world, never()).updateLightByType(EnumSkyBlock.Block, 1, 2, 3);
        verify(world, never()).updateLightByType(EnumSkyBlock.Sky, 1, 2, 3);
    }
}
