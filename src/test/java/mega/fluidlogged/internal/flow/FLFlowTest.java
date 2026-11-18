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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.Test;

class FLFlowTest {

    @Test
    void openFacesAllowFlow() {
        IBlockAccess world = mock(IBlockAccess.class);
        when(world.getBlock(anyInt(), anyInt(), anyInt())).thenReturn(mock(Block.class));

        assertTrue(FLFlow.canPass(world, 0, 0, 0, ForgeDirection.UP));
    }

    @Test
    void aSolidSourceFaceBlocksFlow() {
        IBlockAccess world = mock(IBlockAccess.class);
        Block block = mock(Block.class);
        when(world.getBlock(anyInt(), anyInt(), anyInt())).thenReturn(block);
        when(block.isSideSolid(any(), anyInt(), anyInt(), anyInt(), any(ForgeDirection.class))).thenReturn(true);

        assertFalse(FLFlow.canPass(world, 0, 0, 0, ForgeDirection.UP));
    }
}
