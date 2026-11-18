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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

import net.minecraftforge.fluids.Fluid;

import org.junit.jupiter.api.Test;

class FLManagerSerializationTest {

    @Test
    void namesRoundTripWithoutLosingEmptyCells() {
        Fluid water = new Fluid("water");
        Fluid lava = new Fluid("lava");
        Fluid[] input = { water, null, lava };
        FLManager.FluidNameList serialized = FLManager.serializeNames(input);

        assertNotNull(serialized);
        assertEquals(2, serialized.names.size());

        Fluid[] output = FLManager.deserializeNames(
            input.length,
            serialized.presence,
            serialized.names,
            name -> name.equals("water") ? water : name.equals("lava") ? lava : null);
        assertNotNull(output);
        assertSame(water, output[0]);
        assertNull(output[1]);
        assertSame(lava, output[2]);
    }

    @Test
    void unknownNamesAreDropped() {
        BitSet presence = new BitSet(1);
        presence.set(0);

        assertNull(
            FLManager
                .deserializeNames(1, presence, Collections.singletonList("fluidlogged_missing_fluid"), name -> null));
    }

    @Test
    void fluidsWithoutBlocksAreStillSaved() {
        Fluid fluid = new Fluid("fluidlogged_no_block_fluid");
        FLManager.FluidNameList serialized = FLManager.serializeNames(new Fluid[] { fluid });

        assertNotNull(serialized);
        assertEquals(Collections.singletonList(fluid.getName()), serialized.names);
    }

    @Test
    void emptySectionsSerializeToNull() {
        assertNull(FLManager.serializeNames(new Fluid[4]));
    }

    @Test
    void shortNameListStopsCleanly() {
        BitSet presence = new BitSet(2);
        presence.set(0);
        presence.set(1);

        List<String> names = new ArrayList<>();
        Fluid water = new Fluid("water");
        names.add(water.getName());

        Fluid[] output = FLManager.deserializeNames(2, presence, names, name -> name.equals("water") ? water : null);
        assertNotNull(output);
        assertSame(water, output[0]);
        assertNull(output[1]);
    }
}
