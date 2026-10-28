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

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraftforge.fluids.Fluid;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import mega.fluidlogged.api.FLChunk;
import mega.fluidlogged.internal.mixin.hook.FLSubChunk;

/**
 * Third-party implementations must stay on the conservative path when they do not implement the optional fast-path
 * additions.
 */
class FLFastPathDefaultsTest {

    @Test
    void chunkDefaultsToConservative() {
        FLChunk chunk = new FLChunk() {

            @Override
            public @Nullable Fluid fl$getFluid(int x, int y, int z) {
                return null;
            }

            @Override
            public void fl$setFluid(int x, int y, int z, @Nullable Fluid fluid) {}
        };

        assertTrue(chunk.fl$hasFluid());
        chunk.fl$invalidateHasFluid();
        assertTrue(chunk.fl$hasFluid());
    }

    @Test
    void subChunkDefaultsToConservative() {
        FLSubChunk subChunk = new FLSubChunk() {

            @Override
            public Fluid @Nullable [] fl$getFluidLog() {
                return null;
            }

            @Override
            public void fl$setFluidLog(Fluid @Nullable [] fluidLog) {}

            @Override
            public @Nullable Fluid fl$getFluid(int x, int y, int z) {
                return null;
            }

            @Override
            public void fl$setFluid(int x, int y, int z, @Nullable Fluid fluid) {}
        };

        assertTrue(subChunk.fl$hasFluid());
    }
}
