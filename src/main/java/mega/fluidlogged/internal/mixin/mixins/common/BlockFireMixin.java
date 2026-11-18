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

package mega.fluidlogged.internal.mixin.mixins.common;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import mega.fluidlogged.api.FLBlockAccess;

/**
 * A block holding water neither burns nor spreads fire, as in modern vanilla waterlogging. Ported from LiquidLogged's
 * {@code MixinBlockFire}.
 */
@Mixin(BlockFire.class)
public abstract class BlockFireMixin {

    @WrapOperation(
        method = "tryCatchFire(Lnet/minecraft/world/World;IIIILjava/util/Random;I"
            + "Lnet/minecraftforge/common/util/ForgeDirection;)V",
        remap = false,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;getFlammability"
                + "(Lnet/minecraft/world/IBlockAccess;IIILnet/minecraftforge/common/util/ForgeDirection;)I",
            remap = false),
        require = 1)
    private int fluidlogged$flammability(Block block, IBlockAccess world, int x, int y, int z, ForgeDirection face,
        Operation<Integer> original) {
        return holdsWater(world, x, y, z) ? 0 : original.call(block, world, x, y, z, face);
    }

    @WrapOperation(
        method = "getChanceToEncourageFire",
        remap = false,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;getFireSpreadSpeed"
                + "(Lnet/minecraft/world/IBlockAccess;IIILnet/minecraftforge/common/util/ForgeDirection;)I",
            remap = false),
        require = 1)
    private int fluidlogged$spreadSpeed(Block block, IBlockAccess world, int x, int y, int z, ForgeDirection face,
        Operation<Integer> original) {
        return holdsWater(world, x, y, z) ? 0 : original.call(block, world, x, y, z, face);
    }

    @WrapOperation(
        method = "canCatchFire",
        remap = false,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;isFlammable"
                + "(Lnet/minecraft/world/IBlockAccess;IIILnet/minecraftforge/common/util/ForgeDirection;)Z",
            remap = false),
        require = 1)
    private boolean fluidlogged$flammable(Block block, IBlockAccess world, int x, int y, int z, ForgeDirection face,
        Operation<Boolean> original) {
        return !holdsWater(world, x, y, z) && original.call(block, world, x, y, z, face);
    }

    @Unique
    private static boolean holdsWater(IBlockAccess world, int x, int y, int z) {
        if (!(world instanceof FLBlockAccess)) {
            return false;
        }
        Fluid fluid = ((FLBlockAccess) world).fl$getFluid(x, y, z);
        Block fluidBlock = fluid == null ? null : fluid.getBlock();
        return fluidBlock != null && fluidBlock.getMaterial() == Material.water;
    }
}
