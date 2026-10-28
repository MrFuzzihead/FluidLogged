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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fluids.Fluid;

import org.junit.jupiter.api.Test;

import mega.fluidlogged.api.FLBlockAccess;

class FLUtilTest {

    @Test
    void fluidHelpersPreferFluidLoggedData() {
        Block host = new TestBlock(Material.rock);
        Block fluidBlock = new TestBlock(Material.water);
        Fluid fluid = new Fluid("fluidlogged_test_fluid");
        fluid.setBlock(fluidBlock);

        IBlockAccess access = fakeAccess(host, fluid, 1, 2, 3);

        assertSame(fluidBlock, FLUtil.getFluidOrBlock(access, 1, 2, 3));
        assertSame(host, FLUtil.getFluidOrBlock(access, 4, 5, 6));

        assertEquals(7, FLUtil.getFluidMeta(access, 1, 2, 3, 7));
        assertEquals(3, FLUtil.getFluidMeta(access, 4, 5, 6, 7));
    }

    @Test
    void fluidLightValueUsesLuminosityAndBlockLight() {
        Fluid fluid = new Fluid("fluidlogged_light_test");
        assertEquals(0, FLUtil.fluidLightValue(null));
        assertEquals(0, FLUtil.fluidLightValue(fluid));

        fluid.setLuminosity(10);
        assertEquals(10, FLUtil.fluidLightValue(fluid));

        Block lightBlock = new TestBlock(Material.lava).withLightValue(12);
        fluid.setBlock(lightBlock);
        assertEquals(12, FLUtil.fluidLightValue(fluid));

        fluid.setLuminosity(14);
        assertEquals(14, FLUtil.fluidLightValue(fluid));
    }

    @Test
    void fluidLightValueIsClampedToVanillaRange() {
        Fluid fluid = new Fluid("fluidlogged_light_clamp_test").setLuminosity(20);
        assertEquals(15, FLUtil.fluidLightValue(fluid));

        fluid.setLuminosity(-5);
        assertEquals(0, FLUtil.fluidLightValue(fluid));

        fluid.setBlock(new TestBlock(Material.lava).withLightValue(20));
        assertEquals(15, FLUtil.fluidLightValue(fluid));
    }

    private static IBlockAccess fakeAccess(Block host, Fluid fluid, int fluidX, int fluidY, int fluidZ) {
        InvocationHandler handler = new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                switch (method.getName()) {
                    case "fl$getFluid":
                        return (Integer) args[0] == fluidX && (Integer) args[1] == fluidY && (Integer) args[2] == fluidZ
                            ? fluid
                            : null;
                    case "fl$setFluid":
                        return null;
                    case "getBlock":
                        return host;
                    case "getBlockMetadata":
                        return 3;
                    case "toString":
                        return "FakeFLBlockAccess";
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == args[0];
                    default:
                        return defaultValue(method.getReturnType());
                }
            }
        };
        return (IBlockAccess) Proxy.newProxyInstance(
            FLUtilTest.class.getClassLoader(),
            new Class<?>[] { IBlockAccess.class, FLBlockAccess.class },
            handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type == char.class) return '\0';
        return null;
    }

    private static final class TestBlock extends Block {

        private int lightValue;

        private TestBlock(Material material) {
            super(material);
        }

        private TestBlock withLightValue(int lightValue) {
            this.lightValue = lightValue;
            return this;
        }

        @Override
        public int getLightValue() {
            return lightValue;
        }
    }
}
