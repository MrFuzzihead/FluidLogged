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

package mega.fluidlogged.internal.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.falsepattern.lib.turboasm.ClassNodeHandle;
import com.falsepattern.lib.turboasm.TurboClassTransformer;

import lombok.SneakyThrows;
import lombok.val;
import mega.fluidlogged.FLConstants;

/**
 * Adds a post-return hook to Angelica's chunk block renderer. Angelica owns the render buffers, lighting, shader
 * context, and pass selection, so the hook re-enters Angelica's own renderBlock method for the fluidlogged block.
 */
public class AngelicaFluidLogRendererInjector implements TurboClassTransformer {

    private static final Logger LOGGER = LogManager.getLogger(FLConstants.MOD_NAME);
    private static final String TARGET_CLASS = "com.gtnewhorizons.angelica.rendering.celeritas.AngelicaChunkBuilderMeshingTask";
    private static final String HOOK_OWNER = "mega/fluidlogged/internal/core/ASMHooks";
    private static final String HOOK_NAME = "drawFluidLoggedAngelica";
    private static final String HOOK_DESCRIPTOR = "(Ljava/lang/Object;Lnet/minecraft/block/Block;IIIIILnet/minecraft/client/renderer/Tessellator;"
        + "Lnet/minecraft/client/renderer/RenderBlocks;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;"
        + "IIILjava/lang/Object;ZLjava/lang/Object;J)V";

    @Override
    public String owner() {
        return FLConstants.MOD_ID;
    }

    @Override
    public String name() {
        return "AngelicaFluidLogRendererInjector";
    }

    @Override
    public boolean shouldTransformClass(@NotNull String className, @NotNull ClassNodeHandle classNode) {
        return TARGET_CLASS.equals(className);
    }

    @SneakyThrows
    @Override
    public boolean transformClass(@NotNull String className, @NotNull ClassNodeHandle classNode) {
        val cn = classNode.getNode();
        if (cn == null) {
            return false;
        }

        for (val method : cn.methods) {
            if (!method.name.equals("renderBlock") || Type.getArgumentTypes(method.desc).length != 18) {
                continue;
            }
            if (fl$hasHook(method)) {
                return true;
            }
            boolean injected = false;
            for (val instruction : method.instructions.toArray()) {
                if (instruction.getOpcode() != Opcodes.RETURN) {
                    continue;
                }
                method.instructions.insertBefore(instruction, fl$hook());
                injected = true;
            }
            if (!injected) {
                LOGGER
                    .warn("Angelica renderBlock has no return instruction; FluidLogged renderer hook was not applied");
                return false;
            }
            return true;
        }
        LOGGER.warn("Unsupported Angelica renderBlock signature; FluidLogged renderer hook was not applied");
        return false;
    }

    private static boolean fl$hasHook(MethodNode method) {
        for (val instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode) {
                val call = (MethodInsnNode) instruction;
                if (HOOK_OWNER.equals(call.owner) && HOOK_NAME.equals(call.name) && HOOK_DESCRIPTOR.equals(call.desc)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static InsnList fl$hook() {
        val instructions = new InsnList();
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        for (int slot = 2; slot <= 6; slot++) {
            instructions.add(new VarInsnNode(Opcodes.ILOAD, slot));
        }
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 7));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 8));
        for (int slot = 9; slot <= 11; slot++) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, slot));
        }
        for (int slot = 12; slot <= 14; slot++) {
            instructions.add(new VarInsnNode(Opcodes.ILOAD, slot));
        }
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 15));
        instructions.add(new VarInsnNode(Opcodes.ILOAD, 16));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 17));
        instructions.add(new VarInsnNode(Opcodes.LLOAD, 18));
        instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER, HOOK_NAME, HOOK_DESCRIPTOR, false));
        return instructions;
    }
}
