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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import mega.fluidlogged.api.FLBlockAccess;

class FluidLoggedCommandTest {

    private final FluidLoggedCommand command = new FluidLoggedCommand();

    @Test
    void exposesCommandMetadata() {
        assertEquals("fluidlogged", command.getCommandName());
        assertEquals(2, command.getRequiredPermissionLevel());
        assertEquals(
            "/fluidlogged <get|set|clear> <x> <y> <z> [fluid]",
            command.getCommandUsage(mock(ICommandSender.class)));
    }

    @Test
    void getReportsTheStoredFluid() {
        ICommandSender sender = mock(ICommandSender.class);
        World world = mock(World.class, withSettings().extraInterfaces(FLBlockAccess.class));
        when(sender.getEntityWorld()).thenReturn(world);
        when(sender.getPlayerCoordinates()).thenReturn(new ChunkCoordinates(0, 0, 0));
        Fluid water = new Fluid("fluidlogged_command_water");
        when(((FLBlockAccess) world).fl$getFluid(1, 2, 3)).thenReturn(water);

        command.processCommand(sender, new String[] { "get", "1", "2", "3" });

        ArgumentCaptor<IChatComponent> message = ArgumentCaptor.forClass(IChatComponent.class);
        verify(sender).addChatMessage(message.capture());
        assertEquals(
            "fluidlogged_command_water",
            message.getValue()
                .getUnformattedText());
    }

    @Test
    void getReportsNoneForAnEmptyCell() {
        ICommandSender sender = mock(ICommandSender.class);
        World world = mock(World.class, withSettings().extraInterfaces(FLBlockAccess.class));
        when(sender.getEntityWorld()).thenReturn(world);
        when(sender.getPlayerCoordinates()).thenReturn(new ChunkCoordinates(0, 0, 0));

        command.processCommand(sender, new String[] { "get", "1", "2", "3" });

        ArgumentCaptor<IChatComponent> message = ArgumentCaptor.forClass(IChatComponent.class);
        verify(sender).addChatMessage(message.capture());
        assertEquals(
            "none",
            message.getValue()
                .getUnformattedText());
    }

    @Test
    void clearRemovesTheStoredFluidAndNotifiesNeighbours() {
        ICommandSender sender = mock(ICommandSender.class);
        World world = mock(World.class, withSettings().extraInterfaces(FLBlockAccess.class));
        FLBlockAccess access = (FLBlockAccess) world;
        when(sender.getEntityWorld()).thenReturn(world);
        when(sender.getPlayerCoordinates()).thenReturn(new ChunkCoordinates(0, 0, 0));

        command.processCommand(sender, new String[] { "clear", "1", "2", "3" });

        verify(access).fl$setFluid(1, 2, 3, null);
        verify(world).notifyBlocksOfNeighborChange(1, 2, 3, world.getBlock(1, 2, 3));
        ArgumentCaptor<IChatComponent> message = ArgumentCaptor.forClass(IChatComponent.class);
        verify(sender).addChatMessage(message.capture());
        assertEquals(
            "cleared",
            message.getValue()
                .getUnformattedText());
    }

    @Test
    void rejectsTooFewArguments() {
        ICommandSender sender = mock(ICommandSender.class);
        assertThrows(WrongUsageException.class, () -> command.processCommand(sender, new String[] { "get", "1", "2" }));
    }

    @Test
    void rejectsUnknownSubcommands() {
        ICommandSender sender = mock(ICommandSender.class);
        when(sender.getPlayerCoordinates()).thenReturn(new ChunkCoordinates(0, 0, 0));
        when(sender.getEntityWorld())
            .thenReturn(mock(World.class, withSettings().extraInterfaces(FLBlockAccess.class)));
        assertThrows(
            WrongUsageException.class,
            () -> command.processCommand(sender, new String[] { "wat", "1", "2", "3" }));
    }
}
