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
