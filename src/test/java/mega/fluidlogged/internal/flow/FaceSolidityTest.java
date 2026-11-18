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

import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.Test;

class FaceSolidityTest {

    @Test
    void bottomStairsAreSolidUnderneathOnly() {
        assertTrue(FaceSolidity.isSolid(FaceSolidity.Shape.STAIRS, 0, ForgeDirection.DOWN));
        assertFalse(FaceSolidity.isSolid(FaceSolidity.Shape.STAIRS, 0, ForgeDirection.UP));
    }

    @Test
    void topStairsAreSolidAboveOnly() {
        assertTrue(FaceSolidity.isSolid(FaceSolidity.Shape.STAIRS, 4, ForgeDirection.UP));
        assertFalse(FaceSolidity.isSolid(FaceSolidity.Shape.STAIRS, 4, ForgeDirection.DOWN));
    }

    @Test
    void stairsKeepTheirBackFace() {
        // meta 0 faces east; east is the back face.
        assertTrue(FaceSolidity.isSolid(FaceSolidity.Shape.STAIRS, 0, ForgeDirection.EAST));
        assertFalse(FaceSolidity.isSolid(FaceSolidity.Shape.STAIRS, 0, ForgeDirection.WEST));
    }

    @Test
    void slabsBlockTheirBottomOrTopFace() {
        assertTrue(FaceSolidity.isSolid(FaceSolidity.Shape.SLAB, 0, ForgeDirection.DOWN));
        assertFalse(FaceSolidity.isSolid(FaceSolidity.Shape.SLAB, 0, ForgeDirection.UP));
        assertTrue(FaceSolidity.isSolid(FaceSolidity.Shape.SLAB, 8, ForgeDirection.UP));
        assertFalse(FaceSolidity.isSolid(FaceSolidity.Shape.SLAB, 8, ForgeDirection.DOWN));
    }

    @Test
    void leavesBlockEveryFace() {
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            assertTrue(FaceSolidity.isSolid(FaceSolidity.Shape.LEAVES, 0, side));
        }
    }

    @Test
    void trapdoorsBlockOnlyTheirClosedFace() {
        assertTrue(FaceSolidity.isSolid(FaceSolidity.Shape.TRAPDOOR, 0, ForgeDirection.DOWN));
        assertFalse(FaceSolidity.isSolid(FaceSolidity.Shape.TRAPDOOR, 0, ForgeDirection.UP));
        assertTrue(FaceSolidity.isSolid(FaceSolidity.Shape.TRAPDOOR, 8, ForgeDirection.UP));
        assertFalse(FaceSolidity.isSolid(FaceSolidity.Shape.TRAPDOOR, 8, ForgeDirection.DOWN));

        // Open trapdoor with meta 4 + hinge 0 (south).
        assertTrue(FaceSolidity.isSolid(FaceSolidity.Shape.TRAPDOOR, 4, ForgeDirection.SOUTH));
        assertFalse(FaceSolidity.isSolid(FaceSolidity.Shape.TRAPDOOR, 4, ForgeDirection.DOWN));
    }

    @Test
    void unknownShapesNeverBlock() {
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            assertFalse(FaceSolidity.isSolid(FaceSolidity.Shape.OTHER, 0, side));
        }
    }
}
