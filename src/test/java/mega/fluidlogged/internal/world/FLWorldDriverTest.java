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

package mega.fluidlogged.internal.world;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import net.minecraft.block.Block;
import net.minecraftforge.fluids.Fluid;

import org.junit.jupiter.api.Test;

import mega.fluidlogged.api.world.WorldDriver;

class FLWorldDriverTest {

    @Test
    void acceptsBlocksReportedByARegisteredDriver() {
        FLWorldDriver driver = new FLWorldDriver();
        Block block = mock(Block.class);
        Fluid fluid = new Fluid("fluidlogged_driver_test");
        driver.registerDriver(new WorldDriver() {

            @Override
            public boolean canBeFluidLogged(Block candidate, int meta, Fluid candidateFluid) {
                return candidate == block && candidateFluid == fluid;
            }
        });

        assertTrue(driver.canBeFluidLogged(block, 0, fluid));
        assertFalse(driver.canBeFluidLogged(mock(Block.class), 0, fluid));
    }

    @Test
    void rejectsEverythingWithoutADriver() {
        FLWorldDriver driver = new FLWorldDriver();
        assertFalse(driver.canBeFluidLogged(mock(Block.class), 0, new Fluid("fluidlogged_no_driver")));
    }
}
