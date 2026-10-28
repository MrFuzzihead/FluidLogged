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

package mega.fluidlogged.internal.mixin.mixins.client;

import net.minecraft.client.renderer.Tessellator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Angelica captures deferred geometry straight from the tessellator's raw buffers. These accessors let FluidLogged do
 * the same without an access transformer.
 */
@Mixin(Tessellator.class)
public interface TessellatorAccessorMixin {

    @Accessor("rawBuffer")
    int[] fluidlogged$getRawBuffer();

    @Accessor("vertexCount")
    int fluidlogged$getVertexCount();

    @Accessor("isDrawing")
    void fluidlogged$setDrawing(boolean drawing);

    @Invoker("reset")
    void fluidlogged$reset();
}
