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

package mega.fluidlogged.internal.mixin.mixins.common;

import net.minecraft.world.ChunkCache;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraftforge.fluids.Fluid;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import lombok.val;
import mega.fluidlogged.api.FLBlockAccess;
import mega.fluidlogged.api.FLChunk;

@Mixin(ChunkCache.class)
public abstract class ChunkCacheMixin implements FLBlockAccess {

    @Shadow
    private int chunkX;

    @Shadow
    private Chunk[][] chunkArray;

    @Shadow
    private int chunkZ;

    @Unique
    private boolean fl$hasAnyFluid;
    @Unique
    private boolean fl$hasAnyFluidKnown;

    @Override
    public void fl$setFluid(int x, int y, int z, @Nullable Fluid fluid) {
        if (fluid != null) {
            fl$hasAnyFluid = true;
            fl$hasAnyFluidKnown = true;
        } else {
            fl$hasAnyFluidKnown = false;
        }
        int cX = (x >> 4) - this.chunkX;
        int cZ = (z >> 4) - this.chunkZ;
        if (cX < 0 || cZ < 0 || cX >= chunkArray.length) {
            return;
        }
        val slice = chunkArray[cX];
        if (cZ >= slice.length) {
            return;
        }
        val chunk = slice[cZ];
        if (chunk == null || chunk instanceof EmptyChunk) return;
        ((FLChunk) chunk).fl$setFluid(x & 0xF, y, z & 0xF, fluid);
    }

    @Override
    public @Nullable Fluid fl$getFluid(int x, int y, int z) {
        if (!fl$hasAnyFluid()) return null;
        int cX = (x >> 4) - this.chunkX;
        int cZ = (z >> 4) - this.chunkZ;
        if (cX < 0 || cZ < 0 || cX >= chunkArray.length) {
            return null;
        }
        val slice = chunkArray[cX];
        if (cZ >= slice.length) {
            return null;
        }
        val chunk = slice[cZ];
        if (chunk == null || chunk instanceof EmptyChunk) return null;
        return ((FLChunk) chunk).fl$getFluid(x & 0xF, y, z & 0xF);
    }

    @Unique
    private boolean fl$hasAnyFluid() {
        if (!fl$hasAnyFluidKnown) {
            fl$hasAnyFluid = false;
            fl$hasAnyFluidKnown = true;
            if (chunkArray != null) {
                for (val slices : chunkArray) {
                    if (slices == null) continue;
                    for (val chunk : slices) {
                        if (chunk != null && ((FLChunk) chunk).fl$hasFluid()) {
                            fl$hasAnyFluid = true;
                            return true;
                        }
                    }
                }
            }
        }
        return fl$hasAnyFluid;
    }
}
