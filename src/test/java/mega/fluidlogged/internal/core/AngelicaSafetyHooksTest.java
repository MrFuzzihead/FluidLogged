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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;

import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fluids.Fluid;

import org.junit.jupiter.api.Test;

import mega.fluidlogged.api.FLBlockAccess;

class AngelicaSafetyHooksTest {

    @Test
    void vanillaAndForgeFluidRenderTypesAreThreadSafe() {
        for (int renderType : new int[] { -1, 0, 1, 17, 41 }) {
            assertTrue(
                AngelicaSafetyHooks.isThreadSafeFluidBlock(renderType, false),
                "vanilla render type " + renderType + " should be safe off-thread");
        }
        for (int renderType : new int[] { 42, 43, 255 }) {
            assertTrue(
                AngelicaSafetyHooks.isThreadSafeFluidBlock(renderType, true),
                "Forge fluid render type " + renderType + " should be safe off-thread");
        }
    }

    @Test
    void customFluidRenderTypesAreConservative() {
        for (int renderType : new int[] { -2, 42, 43, 255 }) {
            assertFalse(
                AngelicaSafetyHooks.isThreadSafeFluidBlock(renderType, false),
                "custom fluid render type " + renderType + " must be deferred to the main thread");
        }
    }

    @Test
    void renderPassFallsBackInVanillaOrder() {
        assertEquals(-1, AngelicaSafetyHooks.firstAvailablePass(pass -> false));
        assertEquals(0, AngelicaSafetyHooks.firstAvailablePass(pass -> pass == 0));
        assertEquals(1, AngelicaSafetyHooks.firstAvailablePass(pass -> pass == 1));
        assertEquals(0, AngelicaSafetyHooks.firstAvailablePass(pass -> true));
    }

    @Test
    void nonFluidAccessIsOffThreadSafe() {
        assertTrue(AngelicaSafetyHooks.fluidCanRenderOffThread(null, 0, 0, 0));
    }

    @Test
    void missingFluidIsOffThreadSafe() {
        IBlockAccess access = accessReturning(null);
        assertTrue(AngelicaSafetyHooks.fluidCanRenderOffThread(access, 0, 0, 0));
    }

    @Test
    void fluidWithoutBlockIsOffThreadSafe() {
        IBlockAccess access = accessReturning(new Fluid("fluidlogged_test"));
        assertTrue(AngelicaSafetyHooks.fluidCanRenderOffThread(access, 0, 0, 0));
    }

    private static IBlockAccess accessReturning(Fluid fluid) {
        return (IBlockAccess) Proxy.newProxyInstance(
            AngelicaSafetyHooksTest.class.getClassLoader(),
            new Class<?>[] { IBlockAccess.class, FLBlockAccess.class },
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "fl$getFluid":
                        return fluid;
                    case "fl$setFluid":
                        return null;
                    case "fl$isFluidLogged":
                        return false;
                    default:
                        return primitiveDefault(method.getReturnType());
                }
            });
    }

    private static Object primitiveDefault(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return (char) 0;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }
}
