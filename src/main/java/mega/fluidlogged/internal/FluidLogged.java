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

import net.minecraftforge.common.MinecraftForge;

import com.falsepattern.chunk.api.DataRegistry;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import mega.fluidlogged.FLConstants;
import mega.fluidlogged.Tags;
import mega.fluidlogged.api.bucket.BucketDriver;
import mega.fluidlogged.api.world.WorldDriver;
import mega.fluidlogged.internal.bucket.FLBucketDriver;
import mega.fluidlogged.internal.bucket.drivers.ForgeBucketDriver;
import mega.fluidlogged.internal.world.drivers.MinecraftWorldDriver;

@Mod(
    modid = FLConstants.MOD_ID,
    version = Tags.VERSION,
    name = FLConstants.MOD_NAME,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "required-after:chunkapi@[0.6.4,);" + "required-after:falsepatternlib@[1.7.0,);")
public class FluidLogged {

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        DataRegistry.registerDataManager(new FLManager());
        MinecraftForge.EVENT_BUS.register(FLBucketDriver.INSTANCE);
        BucketDriver.register(new ForgeBucketDriver());
        WorldDriver.register(new MinecraftWorldDriver());
    }
}
