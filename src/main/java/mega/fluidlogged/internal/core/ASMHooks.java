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

    private static final int MAX_RENDER_PASSES = 2;

    private static final ThreadLocal<Boolean> ANGELICA_RENDERING_FLUID = new ThreadLocal<>();

    // Angelica's renderBlock is looked up once per target class instead of once per block. The lookup walks the
    // concrete class hierarchy (so subclass overrides win) and validates the shape of the method before caching it.
    private static final ClassValue<Method> ANGELICA_RENDER_BLOCK = new ClassValue<Method>() {

        @Override
        protected Method computeValue(Class<?> type) {
            Method method = findAngelicaRenderBlock(type);
            if (method == null) {
                throw new IllegalStateException("Unable to find Angelica renderBlock method on " + type.getName());
            }
            method.setAccessible(true);
            return method;
        }
    };

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

        int fluidPass = getFluidRenderPass(fluidBlock);
        if (fluidPass < 0) {
            return;
        }

        int fluidMetadata = fluidBlock instanceof BlockFluidBase
            ? ((BlockFluidBase) fluidBlock).getMaxRenderHeightMeta()
            : 0;

        // Let Angelica select the material from the fluid's own render pass instead of inheriting a host
        // shader-pack override.
        Object[] fluidArgs = { fluidBlock, fluidMetadata, x, y, z, fluidPass, tessellator, renderBlocks, buffers,
            buildContext, blockRenderContext, originX, originY, originZ, null, false, teMap, currentTick };

        // NOTE: a host that Angelica defers is rendered once per host pass on the main thread, so this can still
        // emit the fluid once per host invocation for those blocks. Cross-pass dedupe belongs with the deferred
        // prescan work, where the fluid's thread-safety is also considered.
        ANGELICA_RENDERING_FLUID.set(Boolean.TRUE);
        try {
            invokeAngelicaRenderBlock(target, fluidArgs);
        } finally {
            ANGELICA_RENDERING_FLUID.remove();
        }
    }

    private static int getFluidRenderPass(Block fluidBlock) {
        int pass = fluidBlock.getRenderBlockPass();
        if (fluidBlock.canRenderInPass(pass)) {
            return pass;
        }
        // A few renderers claim support for passes other than their primary one.
        for (int candidate = 0; candidate < MAX_RENDER_PASSES; candidate++) {
            if (fluidBlock.canRenderInPass(candidate)) {
                return candidate;
            }
        }
        return -1;
    }

    private static Method findAngelicaRenderBlock(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (isAngelicaRenderBlock(method)) {
                    return method;
                }
            }
        }
        return null;
    }

    private static boolean isAngelicaRenderBlock(Method method) {
        if (!"renderBlock".equals(method.getName())) {
            return false;
        }
        Class<?>[] parameters = method.getParameterTypes();
        return parameters.length == 18 && parameters[0] == Block.class
            && parameters[1] == int.class
            && parameters[2] == int.class
            && parameters[3] == int.class
            && parameters[4] == int.class
            && parameters[5] == int.class
            && parameters[6] == Tessellator.class
            && parameters[7] == RenderBlocks.class
            && parameters[11] == int.class
            && parameters[12] == int.class
            && parameters[13] == int.class
            && parameters[15] == boolean.class
            && parameters[17] == long.class;
    }

    private static void invokeAngelicaRenderBlock(Object target, Object[] args) {
        try {
            ANGELICA_RENDER_BLOCK.get(target.getClass())
                .invoke(target, args);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to access Angelica renderBlock", e);
        } catch (InvocationTargetException e) {
            rethrowAngelica(e.getCause());
        }
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
