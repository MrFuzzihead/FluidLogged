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

package mega.fluidlogged.internal.core;

import java.util.function.IntPredicate;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.Fluid;

import mega.fluidlogged.api.FLBlockAccess;

/**
 * Called by {@code AngelicaChunkBuilderMeshingTaskMixin} from Angelica's deferred-block prescan.
 */
@SuppressWarnings("unused") // Called by mixin
public final class AngelicaSafetyHooks {

    // Matches Angelica's own "vanilla render types are thread-safe" cutoff in ThreadedAngelicaChunkBuilderMeshingTask.
    private static final int MAX_VANILLA_RENDER_TYPE = 41;
    private static final int MAX_RENDER_PASSES = 2;

    private AngelicaSafetyHooks() {}

    /**
     * Reports whether the fluidlogged fluid at the given position can be rendered off-thread. The host block's own
     * safety has already been evaluated by Angelica.
     */
    public static boolean fluidCanRenderOffThread(IBlockAccess access, int x, int y, int z) {
        if (!(access instanceof FLBlockAccess)) {
            return true;
        }
        Fluid fluid = ((FLBlockAccess) access).fl$getFluid(x, y, z);
        if (fluid == null) {
            return true;
        }
        Block fluidBlock = fluid.getBlock();
        if (fluidBlock == null) {
            return true;
        }
        int renderType = fluidBlock.getRenderType();
        // Forge's RenderBlockFluid is stateless, and Angelica cannot annotate Forge's own ISBRH.
        // Custom fluid ISBRHs are conservatively deferred to the main thread.
        return isThreadSafeFluidBlock(renderType, fluidBlock instanceof BlockFluidBase);
    }

    static boolean isThreadSafeFluidBlock(int renderType, boolean forgeFluidBlock) {
        if (renderType == -1 || (renderType >= 0 && renderType <= MAX_VANILLA_RENDER_TYPE)) {
            return true;
        }
        return forgeFluidBlock;
    }

    public static int fluidRenderPass(Block fluidBlock) {
        int pass = fluidBlock.getRenderBlockPass();
        if (pass >= 0 && pass < MAX_RENDER_PASSES && fluidBlock.canRenderInPass(pass)) {
            return pass;
        }
        return firstAvailablePass(fluidBlock::canRenderInPass);
    }

    static int firstAvailablePass(IntPredicate canRenderInPass) {
        for (int candidate = 0; candidate < MAX_RENDER_PASSES; candidate++) {
            if (canRenderInPass.test(candidate)) {
                return candidate;
            }
        }
        return -1;
    }

    public static int fluidMetadata(Block fluidBlock) {
        return fluidBlock instanceof BlockFluidBase ? ((BlockFluidBase) fluidBlock).getMaxRenderHeightMeta() : 0;
    }
}
