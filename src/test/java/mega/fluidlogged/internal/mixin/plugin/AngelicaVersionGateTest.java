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

package mega.fluidlogged.internal.mixin.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.falsepattern.lib.mixin.IMixin;

class AngelicaVersionGateTest {

    @Test
    void gateMatchesSupportedAngelicaVersions() {
        Predicate<String> gate = TargetedMod.ANGELICA.getCondition();
        assertTrue(gate.test("angelica-2.1.60"));
        assertTrue(gate.test("angelica-2.2.19"));
        assertTrue(gate.test("angelica-2.2.40"));
        assertTrue(gate.test("angelica-2.2.40-dev"));
        assertFalse(gate.test("angelica-2.10.0"));
        assertFalse(gate.test("angelica-2.0.0"));
        assertFalse(gate.test("angelica-3.0.0"));
    }

    @Test
    void deferredGateMatchesOnlyAngelica2240() {
        Predicate<String> gate = TargetedMod.ANGELICA_2_2_40.getCondition();
        assertTrue(gate.test("angelica-2.2.40"));
        assertTrue(gate.test("angelica-2.2.40-dev"));
        assertTrue(gate.test("angelica-2.2.40-beta"));
        assertTrue(gate.test("angelica-2.2.40.jar"));
        assertTrue(gate.test("angelica-2.2.40-dev.jar"));
        assertFalse(gate.test("angelica-2.1.60"));
        assertFalse(gate.test("angelica-2.2.19"));
        assertFalse(gate.test("angelica-2.2.41"));
        assertFalse(gate.test("angelica-2.2.400"));
        assertFalse(gate.test("angelica-2.3.0"));
    }

    @Test
    void deferredMixinsAreClientOnlyAndRequireAngelica2240() {
        assertDeferredMixin(Mixin.client_AngelicaChunkBuilderMeshingTaskMixin, "AngelicaChunkBuilderMeshingTaskMixin");
        assertDeferredMixin(Mixin.client_TessellatorAccessorMixin, "TessellatorAccessorMixin");
    }

    private static void assertDeferredMixin(Mixin mixin, String name) {
        assertEquals(IMixin.Side.CLIENT, mixin.getSide());
        assertEquals(name, mixin.getMixin());
        assertTrue(
            mixin.getFilter()
                .test(Collections.singletonList(TargetedMod.ANGELICA_2_2_40)));
        assertFalse(
            mixin.getFilter()
                .test(Collections.singletonList(TargetedMod.ANGELICA)));
        assertFalse(
            mixin.getFilter()
                .test(Collections.emptyList()));
    }

    @Test
    void rendererMixinIsClientOnlyAndRequiresAngelica() {
        assertEquals(IMixin.Side.CLIENT, Mixin.client_AngelicaRenderBlockMixin.getSide());
        assertEquals("AngelicaRenderBlockMixin", Mixin.client_AngelicaRenderBlockMixin.getMixin());
        assertTrue(
            Mixin.client_AngelicaRenderBlockMixin.getFilter()
                .test(Collections.singletonList(TargetedMod.ANGELICA)));
        assertFalse(
            Mixin.client_AngelicaRenderBlockMixin.getFilter()
                .test(Collections.emptyList()));
    }
}
