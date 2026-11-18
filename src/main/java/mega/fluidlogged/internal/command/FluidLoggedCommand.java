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

package mega.fluidlogged.internal.command;

import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import mega.fluidlogged.api.FLBlockAccess;
import mega.fluidlogged.api.world.WorldDriver;
import mega.fluidlogged.internal.FLUtil;

/**
 * Operator command to inspect and change stored fluids: {@code /fluidlogged get|set|clear <x> <y> <z> [fluid]}.
 * Ported from LiquidLogged's command.
 */
public class FluidLoggedCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "fluidlogged";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/fluidlogged <get|set|clear> <x> <y> <z> [fluid]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 4) {
            throw new WrongUsageException(getCommandUsage(sender));
        }
        ChunkCoordinates here = sender.getPlayerCoordinates();
        int x = MathHelper.floor_double(func_110666_a(sender, here.posX, args[1]));
        int y = MathHelper.floor_double(func_110666_a(sender, here.posY, args[2]));
        int z = MathHelper.floor_double(func_110666_a(sender, here.posZ, args[3]));
        World world = sender.getEntityWorld();
        FLBlockAccess access = (FLBlockAccess) world;
        switch (args[0]) {
            case "get": {
                Fluid fluid = access.fl$getFluid(x, y, z);
                reply(sender, fluid == null ? "none" : fluid.getName());
                break;
            }
            case "set": {
                if (args.length < 5) {
                    throw new WrongUsageException(getCommandUsage(sender));
                }
                Fluid fluid = FluidRegistry.getFluid(args[4]);
                if (fluid == null) {
                    reply(sender, "unknown fluid " + args[4]);
                    return;
                }
                Block block = world.getBlock(x, y, z);
                if (!WorldDriver.getCanBeFluidLogged(block, world.getBlockMetadata(x, y, z), fluid)) {
                    reply(sender, "not allowed");
                    return;
                }
                access.fl$setFluid(x, y, z, fluid);
                Block fluidBlock = fluid.getBlock();
                if (fluidBlock != null) {
                    FLUtil.onFluidPlacedInto(world, x, y, z, block, fluidBlock);
                }
                world.notifyBlocksOfNeighborChange(x, y, z, block);
                reply(sender, "set");
                break;
            }
            case "clear": {
                Block block = world.getBlock(x, y, z);
                access.fl$setFluid(x, y, z, null);
                world.notifyBlocksOfNeighborChange(x, y, z, block);
                reply(sender, "cleared");
                break;
            }
            default:
                throw new WrongUsageException(getCommandUsage(sender));
        }
    }

    private static void reply(ICommandSender sender, String text) {
        sender.addChatMessage(new ChatComponentText(text));
    }
}
