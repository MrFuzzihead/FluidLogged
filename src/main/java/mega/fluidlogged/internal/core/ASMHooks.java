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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.Fluid;

import lombok.val;
import mega.fluidlogged.api.FLBlockAccess;

@SuppressWarnings("unused") // Called by ASM
public class ASMHooks {

    public static final int BIT_NEXT_PASS = 0b1;
    public static final int BIT_RENDERED_ANYTHING = 0b10;

    private static final ThreadLocal<Boolean> ANGELICA_RENDERING_FLUID = new ThreadLocal<>();

    public static int drawFluidLogged(RenderBlocks renderBlocks, int x, int y, int z, int pass) {

        val fluid = ((FLBlockAccess) renderBlocks.blockAccess).fl$getFluid(x, y, z);
        val fluidBlock = fluid == null ? null : fluid.getBlock();

        int result = 0;

        if (pass < 1 && fluidBlock != null && fluidBlock.getRenderBlockPass() > 0) {
            result |= 0b1;
        }
        if (fluidBlock != null && fluidBlock.canRenderInPass(pass)
            && renderBlocks.renderBlockByRenderType(fluidBlock, x, y, z)) {
            result |= 0b10;
        }

        return result;
    }

    public static void drawFluidLoggedAngelica(Object target, Block block, int metadata, int x, int y, int z, int pass,
        Tessellator tessellator, RenderBlocks renderBlocks, Object buffers, Object buildContext,
        Object blockRenderContext, int originX, int originY, int originZ, Object materialOverride,
        boolean isShaderPackOverride, Object teMap, long currentTick) {
        if (Boolean.TRUE.equals(ANGELICA_RENDERING_FLUID)) {
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

        Object[] args = { block, metadata, x, y, z, pass, tessellator, renderBlocks, buffers, buildContext,
            blockRenderContext, originX, originY, originZ, materialOverride, isShaderPackOverride, teMap, currentTick };
        int fluidMetadata = fluidBlock instanceof BlockFluidBase
            ? ((BlockFluidBase) fluidBlock).getMaxRenderHeightMeta()
            : 0;

        ANGELICA_RENDERING_FLUID.set(Boolean.TRUE);
        try {
            for (int fluidPass = 0; fluidPass < 2; fluidPass++) {
                if (!fluidBlock.canRenderInPass(fluidPass)) {
                    continue;
                }
                Object[] fluidArgs = args.clone();
                fluidArgs[0] = fluidBlock;
                fluidArgs[1] = fluidMetadata;
                fluidArgs[5] = fluidPass;
                // Let Angelica select the material from the fluid's own render pass instead of inheriting a host
                // shader-pack override.
                fluidArgs[14] = null;
                fluidArgs[15] = false;
                invokeAngelicaRenderBlock(target, fluidArgs);
            }
        } finally {
            ANGELICA_RENDERING_FLUID.remove();
        }
    }

    private static void invokeAngelicaRenderBlock(Object target, Object[] args) {
        Class<?> type = target.getClass();
        while (type != null) {
            for (Method method : type.getDeclaredMethods()) {
                if (!method.getName()
                    .equals("renderBlock") || method.getParameterTypes().length != args.length) {
                    continue;
                }
                try {
                    method.setAccessible(true);
                    method.invoke(target, args);
                    return;
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Unable to access Angelica renderBlock", e);
                } catch (InvocationTargetException e) {
                    rethrowAngelica(e.getCause());
                }
            }
            type = type.getSuperclass();
        }
        throw new IllegalStateException("Unable to find Angelica renderBlock method");
    }

    private static void rethrowAngelica(Throwable throwable) {
        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        }
        if (throwable instanceof Error) {
            throw (Error) throwable;
        }
        throw new IllegalStateException("Angelica renderBlock failed", throwable);
    }
}
