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
import net.minecraft.entity.Entity;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import mega.fluidlogged.api.FLBlockAccess;

/**
 * A cell resists an explosion as much as the stronger of its block and its stored fluid, matching modern vanilla
 * waterlogging. Ported from LiquidLogged's {@code MixinExplosion}.
 */
@Mixin(Explosion.class)
public abstract class ExplosionMixin {

    @WrapOperation(
        method = "doExplosionA",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;getExplosionResistance"
                + "(Lnet/minecraft/entity/Entity;Lnet/minecraft/world/World;IIIDDD)F",
            remap = false),
        require = 1)
    private float fluidlogged$blockResistance(Block block, Entity exploder, World world, int x, int y, int z,
        double explosionX, double explosionY, double explosionZ, Operation<Float> original) {
        float resistance = original.call(block, exploder, world, x, y, z, explosionX, explosionY, explosionZ);
        Block fluidBlock = fluidBlock(world, x, y, z);
        return fluidBlock == null ? resistance
            : Math.max(
                resistance,
                original.call(fluidBlock, exploder, world, x, y, z, explosionX, explosionY, explosionZ));
    }

    @WrapOperation(
        method = "doExplosionA",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;func_145772_a"
                + "(Lnet/minecraft/world/Explosion;Lnet/minecraft/world/World;IIILnet/minecraft/block/Block;)F"),
        require = 1)
    private float fluidlogged$entityResistance(Entity entity, Explosion explosion, World world, int x, int y, int z,
        Block block, Operation<Float> original) {
        float resistance = original.call(entity, explosion, world, x, y, z, block);
        Block fluidBlock = fluidBlock(world, x, y, z);
        return fluidBlock == null ? resistance
            : Math.max(resistance, original.call(entity, explosion, world, x, y, z, fluidBlock));
    }

    private static Block fluidBlock(World world, int x, int y, int z) {
        Fluid fluid = ((FLBlockAccess) world).fl$getFluid(x, y, z);
        return fluid == null ? null : fluid.getBlock();
    }
}
