/*
 * This file is part of FluidLogged.
 * Copyright (C) 2025 The MEGA Team, FalsePattern
 * All Rights Reserved
 * The above copyright notice and this permission notice and the word "MEGA"
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

package mega.fluidlogged.internal.mixin.mixins.common.compat.archaicfix;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import mega.fluidlogged.api.FLBlockAccess;
import mega.fluidlogged.internal.FLUtil;

/**
 * ArchaicFix replaces vanilla light propagation, so its engine needs the stored fluid's opacity and
 * luminosity as well. This is a string target so ArchaicFix is not a compile-time dependency.
 */
@Pseudo
@Mixin(targets = "org.embeddedt.archaicfix.lighting.world.lighting.LightingEngine", remap = false)
public abstract class MixinLightingEngine {

    @WrapOperation(
        method = "getPosOpacity",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;getLightOpacity(Lnet/minecraft/world/IBlockAccess;III)I",
            remap = false))
    private int fluidlogged$opacity(Block block, IBlockAccess world, int x, int y, int z, Operation<Integer> original) {
        return Math.max(
            original.call(block, world, x, y, z),
            FLUtil.fluidLightOpacity(((FLBlockAccess) world).fl$getFluid(x, y, z)));
    }

    @WrapOperation(
        method = "getCursorLuminosity",
        at = @At(
            value = "INVOKE",
            target = "Lorg/embeddedt/archaicfix/lighting/world/lighting/LightingEngineHelpers;"
                + "getLightValueForState(Lnet/minecraft/block/Block;Lnet/minecraft/world/IBlockAccess;III)I",
            remap = false))
    private int fluidlogged$lightValue(Block block, IBlockAccess world, int x, int y, int z,
        Operation<Integer> original) {
        return Math.max(
            original.call(block, world, x, y, z),
            FLUtil.fluidLightValue(((FLBlockAccess) world).fl$getFluid(x, y, z)));
    }
}
