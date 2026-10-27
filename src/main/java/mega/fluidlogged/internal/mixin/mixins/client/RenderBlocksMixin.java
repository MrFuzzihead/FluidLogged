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

package mega.fluidlogged.internal.mixin.mixins.client;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fluids.Fluid;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.llamalad7.mixinextras.sugar.Local;

import mega.fluidlogged.api.FLBlockAccess;
import mega.fluidlogged.internal.FLUtil;

@Mixin(RenderBlocks.class)
public abstract class RenderBlocksMixin {

    @Redirect(
        method = "getLiquidHeight",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/IBlockAccess;getBlock(III)Lnet/minecraft/block/Block;"),
        require = 1)
    private Block hijackGetBlock(IBlockAccess instance, int x, int y, int z,
        @Local(argsOnly = true) Material material) {
        if (instance instanceof FLBlockAccess) {
            Fluid fluid = ((FLBlockAccess) instance).fl$getFluid(x, y, z);
            if (fluid != null) {
                Block fluidBlock = fluid.getBlock();
                // Vanilla getLiquidHeight treats non-solid neighbors as empty space. A different fluidlogged fluid
                // should obstruct the surface instead, so substitute a solid block for the height calculation only.
                if (fluidBlock != null && fluidBlock.getMaterial() != material) {
                    return Blocks.stone;
                }
            }
        }
        return FLUtil.getFluidOrBlock(instance, x, y, z);
    }

    @Redirect(
        method = "getLiquidHeight",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/IBlockAccess;getBlockMetadata(III)I"),
        require = 1)
    private int hijackMeta(IBlockAccess instance, int x, int y, int z) {
        return FLUtil.getFluidMeta(instance, x, y, z, 0);
    }

    @Redirect(
        method = "renderBlockLiquid",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/IBlockAccess;getBlockMetadata(III)I"),
        require = 1)
    private int renderBlockLiquidMeta(IBlockAccess instance, int x, int y, int z) {
        return FLUtil.getFluidMeta(instance, x, y, z, 0);
    }
}
