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

package mega.fluidlogged.api.event;

import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;

import org.jetbrains.annotations.Nullable;

import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;

/** Fires on the Forge bus before a block's stored fluid changes; cancel it to keep the old fluid. */
@Cancelable
public class FluidLogEvent extends Event {

    public final World world;
    public final int x;
    public final int y;
    public final int z;
    public final @Nullable Fluid oldFluid;
    public final @Nullable Fluid newFluid;

    public FluidLogEvent(World world, int x, int y, int z, @Nullable Fluid oldFluid, @Nullable Fluid newFluid) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.oldFluid = oldFluid;
        this.newFluid = newFluid;
    }

    @Override
    public boolean isCancelable() {
        return true;
    }
}
