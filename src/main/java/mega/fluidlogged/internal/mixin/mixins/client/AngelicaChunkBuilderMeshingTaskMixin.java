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

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fluids.Fluid;

import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildOutput;
import org.embeddedt.embeddium.impl.render.chunk.compile.executor.ChunkJobResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizons.angelica.rendering.StateAwareTessellator;
import com.gtnewhorizons.angelica.rendering.celeritas.AngelicaChunkBuildContext;
import com.gtnewhorizons.angelica.rendering.celeritas.AngelicaChunkBuilderMeshingTask;
import com.gtnewhorizons.angelica.rendering.celeritas.DeferredBlock;
import com.gtnewhorizons.angelica.rendering.celeritas.DeferredSectionMesh;
import com.gtnewhorizons.angelica.rendering.celeritas.SmoothBiomeColorCache;
import com.gtnewhorizons.angelica.rendering.celeritas.threading.RenderPassHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import mega.fluidlogged.api.FLBlockAccess;
import mega.fluidlogged.internal.core.AngelicaSafetyHooks;

/**
 * Angelica's deferred-block prescan only checks the host block's ISBRH safety. FluidLogged renders the fluidlogged
 * fluid through a separate ISBRH, so a thread-safe host can still cause a non-thread-safe fluid renderer to run on a
 * worker thread.
 * <p>
 * The prescan's {@code canRenderOffThread} result is kept, and the fluid at the scanned position is checked as well.
 * The injection is optional ({@code require = 0}) so a future Angelica layout change degrades to the previous
 * host-only behaviour.
 * <p>
 * Angelica 2.2.40 renders deferred blocks through {@code drawBlock}/{@code captureDeferred} instead of
 * {@code renderBlock}, so the normal renderer hook never sees them. The captureDeferred injection renders the
 * fluidlogged fluid on the main thread and appends it as a regular deferred capture entry.
 */
@Mixin(AngelicaChunkBuilderMeshingTask.class)
public abstract class AngelicaChunkBuilderMeshingTaskMixin {

    @Shadow(remap = false)
    @Final
    protected RenderSection render;

    @WrapOperation(
        method = "prescanDeferred",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizons/angelica/rendering/celeritas/AngelicaChunkBuilderMeshingTask;"
                + "canRenderOffThread(Lnet/minecraft/block/Block;)Z"),
        require = 0,
        expect = 1,
        remap = false)
    private boolean fluidlogged$canRenderOffThread(AngelicaChunkBuilderMeshingTask task, Block block,
        Operation<Boolean> original, @Local(argsOnly = true) IBlockAccess region,
        @Local(argsOnly = true, ordinal = 0) int minX, @Local(argsOnly = true, ordinal = 1) int minY,
        @Local(argsOnly = true, ordinal = 2) int minZ, @Local(name = "x") int x, @Local(name = "y") int y,
        @Local(name = "z") int z) {
        return original.call(task, block)
            && AngelicaSafetyHooks.fluidCanRenderOffThread(region, minX + x, minY + y, minZ + z);
    }

    @Inject(method = "captureDeferred", at = @At("RETURN"), require = 0, expect = 1, remap = false)
    private void fluidlogged$renderDeferredFluids(DeferredSectionMesh mesh,
        CallbackInfoReturnable<ChunkJobResult<ChunkBuildOutput>> cir) {
        if (cir.getReturnValue() != null || mesh.capture == null) {
            return;
        }
        IBlockAccess access = mesh.slice;
        if (!(access instanceof FLBlockAccess)) {
            return;
        }
        List<DeferredBlock> blocks = mesh.blocks;
        int originalSize = blocks.size();
        if (originalSize == 0) {
            return;
        }

        Tessellator tessellator = Tessellator.instance;
        if (!(tessellator instanceof TessellatorAccessorMixin)) {
            return;
        }
        TessellatorAccessorMixin tessellatorAccessor = (TessellatorAccessorMixin) tessellator;
        StateAwareTessellator stateTessellator = (StateAwareTessellator) tessellator;
        RenderBlocks renderBlocks = new RenderBlocks(access);
        int minX = this.render.getOriginX();
        int minY = this.render.getOriginY();
        int minZ = this.render.getOriginZ();

        stateTessellator.angelica$setCeleritasMeshing(true);
        SmoothBiomeColorCache.setActiveCache(mesh.slice.getBiomeColorCache());
        tessellator.setTranslation(-minX, -minY, -minZ);
        try {
            for (int i = 0; i < originalSize; i++) {
                DeferredBlock deferred = blocks.get(i);
                int x = deferred.x();
                int y = deferred.y();
                int z = deferred.z();
                Fluid fluid = ((FLBlockAccess) access).fl$getFluid(x, y, z);
                if (fluid == null) {
                    continue;
                }
                Block fluidBlock = fluid.getBlock();
                if (fluidBlock == null || fluidBlock == deferred.block()) {
                    continue;
                }
                int fluidPass = AngelicaSafetyHooks.fluidRenderPass(fluidBlock);
                if (fluidPass < 0) {
                    continue;
                }

                mesh.slice.setRenderingBlock(fluidBlock);
                drawBlock(fluidBlock, x, y, z, fluidPass, tessellator, renderBlocks);
                int vertexCount = tessellatorAccessor.fluidlogged$getVertexCount();
                if (vertexCount > 0) {
                    int index = blocks.size();
                    blocks.add(
                        new DeferredBlock(
                            x,
                            y,
                            z,
                            fluidBlock,
                            AngelicaSafetyHooks.fluidMetadata(fluidBlock),
                            1 << fluidPass,
                            null));
                    mesh.capture.add(
                        index * AngelicaChunkBuildContext.NUM_PASSES + fluidPass,
                        tessellatorAccessor.fluidlogged$getRawBuffer(),
                        vertexCount,
                        stateTessellator.angelica$getVertexStates(),
                        stateTessellator.angelica$getShaderOverrideBlockIds(),
                        -1);
                }
                tessellatorAccessor.fluidlogged$reset();
                tessellatorAccessor.fluidlogged$setDrawing(false);
            }
        } catch (Throwable t) {
            // captureDeferred has already completed its own error handling, so report the fluid pass as a failed job
            // instead of letting the exception escape to the scheduler.
            cir.setReturnValue(new ChunkJobResult.Failure<>(t));
        } finally {
            tessellatorAccessor.fluidlogged$reset();
            tessellatorAccessor.fluidlogged$setDrawing(false);
            stateTessellator.angelica$setCeleritasMeshing(false);
            SmoothBiomeColorCache.clearActiveCache();
            tessellator.setTranslation(0, 0, 0);
            mesh.slice.setRenderingBlock(null);
            RenderPassHelper.resetWorldRenderPass();
        }
    }

    @Shadow(remap = false)
    private void drawBlock(Block block, int x, int y, int z, int pass, Tessellator tessellator,
        RenderBlocks renderBlocks) {}
}
