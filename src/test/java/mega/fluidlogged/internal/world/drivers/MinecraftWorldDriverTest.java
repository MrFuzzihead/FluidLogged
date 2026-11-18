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

package mega.fluidlogged.internal.world.drivers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockWall;
import net.minecraftforge.fluids.Fluid;

import org.junit.jupiter.api.Test;

class MinecraftWorldDriverTest {

    private final MinecraftWorldDriver driver = new MinecraftWorldDriver();

    @Test
    void rejectsOpaqueBlocks() {
        Block block = mock(Block.class);
        when(block.isOpaqueCube()).thenReturn(true);

        assertFalse(driver.canBeFluidLogged(block, 0, water(300)));
    }

    @Test
    void rejectsPlainNonOpaqueBlocks() {
        Block block = mock(Block.class);

        assertFalse(driver.canBeFluidLogged(block, 0, water(300)));
    }

    @Test
    void acceptsModdedStairs() {
        Block stairs = mock(BlockStairs.class);

        assertTrue(driver.canBeFluidLogged(stairs, 0, water(300)));
    }

    @Test
    void acceptsWallsAndLeaves() {
        assertTrue(driver.canBeFluidLogged(mock(BlockWall.class), 0, water(300)));
        assertTrue(driver.canBeFluidLogged(mock(BlockLeaves.class), 0, water(300)));
    }

    @Test
    void acceptsLeavesWhenTheServerReportsThemOpaque() {
        BlockLeaves leaves = mock(BlockLeaves.class);
        when(leaves.isOpaqueCube()).thenReturn(true);

        assertTrue(driver.canBeFluidLogged(leaves, 0, water(300)));
    }

    @Test
    void onlyAllowsLavaInTheConfiguredStoneBlocks() {
        Block wall = mock(BlockWall.class);

        assertFalse(driver.canBeFluidLogged(wall, 0, water(1000)));
        assertTrue(driver.canBeFluidLogged(wall, 0, water(300)));
    }

    private static Fluid water(int temperature) {
        return new Fluid("fluidlogged_driver_test").setTemperature(temperature);
    }
}
