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

import net.coderbot.iris.block_rendering.NbtConditionalIdMap;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.Fluid;

import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.angelica.rendering.celeritas.AngelicaChunkBuildContext;
import com.gtnewhorizons.angelica.rendering.celeritas.AngelicaChunkBuilderMeshingTask;
import com.gtnewhorizons.angelica.rendering.celeritas.iris.BlockRenderContext;

import mega.fluidlogged.api.FLBlockAccess;

/**
 * Renders the fluidlogged fluid after the host block. Angelica owns the render buffers, lighting, shader context and
 * pass selection, so the fluid is rendered by re-entering the task's own {@code renderBlock} with the fluid block.
 * <p>
 * This replaces the previous ASM/reflection hook: the recursive call is a direct shadow call, and a per-task guard
 * prevents the RETURN injection from recursing into itself.
 */
@Mixin(AngelicaChunkBuilderMeshingTask.class)
public abstract class AngelicaRenderBlockMixin {

    @Unique
    private boolean fl$renderingFluid;

    @Shadow(remap = false)
    protected abstract void renderBlock(Block block, int metadata, int x, int y, int z, int pass,
        Tessellator tessellator, RenderBlocks renderBlocks, ChunkBuildBuffers buffers,
        AngelicaChunkBuildContext buildContext, BlockRenderContext blockRenderContext, int originX, int originY,
        int originZ, Material materialOverride, boolean isShaderPackOverride, NbtConditionalIdMap<Block> teMap,
        long currentTick);

    @Inject(method = "renderBlock", at = @At("RETURN"), require = 0, expect = 1, remap = false)
    private void fluidlogged$renderFluid(Block block, int metadata, int x, int y, int z, int pass,
        Tessellator tessellator, RenderBlocks renderBlocks, ChunkBuildBuffers buffers,
        AngelicaChunkBuildContext buildContext, BlockRenderContext blockRenderContext, int originX, int originY,
        int originZ, Material materialOverride, boolean isShaderPackOverride, NbtConditionalIdMap<Block> teMap,
        long currentTick, CallbackInfo ci) {
        if (fl$renderingFluid) {
            return;
        }
        IBlockAccess access = renderBlocks.blockAccess;
        if (!(access instanceof FLBlockAccess) || block != access.getBlock(x, y, z)) {
            return;
        }
        Fluid fluid = ((FLBlockAccess) access).fl$getFluid(x, y, z);
        if (fluid == null) {
            return;
        }
        Block fluidBlock = fluid.getBlock();
        if (fluidBlock == null || fluidBlock == block) {
            return;
        }
        int fluidPass = fluidlogged$fluidPass(fluidBlock);
        if (fluidPass < 0) {
            return;
        }
        int fluidMetadata = fluidBlock instanceof BlockFluidBase
            ? ((BlockFluidBase) fluidBlock).getMaxRenderHeightMeta()
            : 0;

        fl$renderingFluid = true;
        try {
            renderBlock(
                fluidBlock,
                fluidMetadata,
                x,
                y,
                z,
                fluidPass,
                tessellator,
                renderBlocks,
                buffers,
                buildContext,
                blockRenderContext,
                originX,
                originY,
                originZ,
                null,
                false,
                teMap,
                currentTick);
        } finally {
            fl$renderingFluid = false;
        }
    }

    @Unique
    private static int fluidlogged$fluidPass(Block fluidBlock) {
        int pass = fluidBlock.getRenderBlockPass();
        if (fluidBlock.canRenderInPass(pass)) {
            return pass;
        }
        for (int candidate = 0; candidate < 2; candidate++) {
            if (fluidBlock.canRenderInPass(candidate)) {
                return candidate;
            }
        }
        return -1;
    }
}
