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

package mega.fluidlogged.internal.bucket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.fluids.Fluid;

import org.junit.jupiter.api.Test;

import cpw.mods.fml.common.eventhandler.Event;
import mega.fluidlogged.api.FLBlockAccess;
import mega.fluidlogged.api.bucket.BucketDriver;
import mega.fluidlogged.api.bucket.BucketEmptyResults;
import mega.fluidlogged.api.bucket.BucketState;
import mega.fluidlogged.api.world.WorldDriver;
import mega.fluidlogged.internal.world.FLWorldDriver;

class FLBucketDriverTest {

    @Test
    void rejectsDriversThatImplementNoRole() {
        FLBucketDriver driver = new FLBucketDriver();
        assertThrows(IllegalArgumentException.class, () -> driver.registerDriver(new BucketDriver() {}));
    }

    @Test
    void ignoresNonBlockHits() {
        FLBucketDriver driver = new FLBucketDriver();
        MovingObjectPosition hit = new MovingObjectPosition(1, 2, 3, 1, Vec3.createVectorHelper(0, 0, 0));
        hit.typeOfHit = MovingObjectPosition.MovingObjectType.ENTITY;
        FillBucketEvent event = new FillBucketEvent(
            mock(EntityPlayer.class),
            mock(ItemStack.class),
            mock(World.class),
            hit);

        driver.onBucketEvent(event);

        assertNull(event.result);
    }

    @Test
    void emptyBucketTakesTheStoredFluid() {
        FLBucketDriver driver = new FLBucketDriver();
        Fluid fluid = new Fluid("fluidlogged_bucket_water");
        ItemStack result = mock(ItemStack.class);
        driver.registerDriver(new TestBucketDriver(result));

        World world = mock(World.class, withSettings().extraInterfaces(FLBlockAccess.class));
        FLBlockAccess access = (FLBlockAccess) world;
        when(access.fl$getFluid(1, 2, 3)).thenReturn(fluid);
        MovingObjectPosition hit = new MovingObjectPosition(1, 2, 3, 1, Vec3.createVectorHelper(0, 0, 0));
        FillBucketEvent event = new FillBucketEvent(mock(EntityPlayer.class), mock(ItemStack.class), world, hit);

        driver.onBucketEvent(event);

        verify(access).fl$setFluid(1, 2, 3, null);
        assertSame(result, event.result);
        assertEquals(Event.Result.ALLOW, event.getResult());
    }

    @Test
    void filledBucketPlacesTheFluidInAnAcceptedBlock() {
        FLBucketDriver driver = new FLBucketDriver();
        Block block = mock(Block.class);
        Fluid fluid = new Fluid("fluidlogged_bucket_lava").setBlock(mock(Block.class));
        ItemStack emptyBucket = mock(ItemStack.class);
        driver.registerDriver(new TestDrainDriver(fluid, emptyBucket));
        FLWorldDriver.INSTANCE.registerDriver(new WorldDriver() {

            @Override
            public boolean canBeFluidLogged(Block candidate, int meta, Fluid candidateFluid) {
                return candidate == block && candidateFluid == fluid;
            }
        });

        World world = mock(World.class, withSettings().extraInterfaces(FLBlockAccess.class));
        FLBlockAccess access = (FLBlockAccess) world;
        when(world.getBlock(1, 2, 3)).thenReturn(block);
        MovingObjectPosition hit = new MovingObjectPosition(1, 2, 3, 1, Vec3.createVectorHelper(0, 0, 0));
        FillBucketEvent event = new FillBucketEvent(mock(EntityPlayer.class), mock(ItemStack.class), world, hit);

        driver.onBucketEvent(event);

        verify(access).fl$setFluid(1, 2, 3, fluid);
        assertSame(emptyBucket, event.result);
        assertEquals(Event.Result.ALLOW, event.getResult());
    }

    private static final class TestBucketDriver implements BucketDriver.Query, BucketDriver.Fill {

        private final ItemStack result;

        private TestBucketDriver(ItemStack result) {
            this.result = result;
        }

        @Override
        public BucketState queryState(ItemStack bucket) {
            return BucketState.Empty;
        }

        @Override
        public ItemStack fillBucket(Fluid fluid, ItemStack bucket) {
            return result;
        }
    }

    private static final class TestDrainDriver implements BucketDriver.Query, BucketDriver.Empty {

        private final Fluid fluid;
        private final ItemStack emptyBucket;

        private TestDrainDriver(Fluid fluid, ItemStack emptyBucket) {
            this.fluid = fluid;
            this.emptyBucket = emptyBucket;
        }

        @Override
        public BucketState queryState(ItemStack bucket) {
            return BucketState.Filled;
        }

        @Override
        public BucketEmptyResults emptyBucket(ItemStack bucket) {
            return new BucketEmptyResults(emptyBucket, fluid);
        }
    }
}
