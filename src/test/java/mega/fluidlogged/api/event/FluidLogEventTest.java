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

package mega.fluidlogged.api.event;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;

import org.junit.jupiter.api.Test;

class FluidLogEventTest {

    @Test
    void exposesTheCellAndFluidsAndIsCancelable() {
        World world = mock(World.class);
        Fluid oldFluid = new Fluid("fluidlogged_event_old");
        Fluid newFluid = new Fluid("fluidlogged_event_new");
        FluidLogEvent event = new FluidLogEvent(world, 1, 2, 3, oldFluid, newFluid);

        assertSame(world, event.world);
        assertSame(oldFluid, event.oldFluid);
        assertSame(newFluid, event.newFluid);
        assertFalse(event.isCanceled());
        event.setCanceled(true);
        assertTrue(event.isCanceled());
    }
}
