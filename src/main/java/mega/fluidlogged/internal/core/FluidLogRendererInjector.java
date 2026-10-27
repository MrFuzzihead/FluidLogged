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
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.falsepattern.lib.asm.ASMUtil;
import com.falsepattern.lib.mapping.MappingManager;
import com.falsepattern.lib.mapping.types.MappingType;
import com.falsepattern.lib.mapping.types.NameType;
import com.falsepattern.lib.turboasm.ClassNodeHandle;
import com.falsepattern.lib.turboasm.TurboClassTransformer;

import lombok.SneakyThrows;
import lombok.val;
import mega.fluidlogged.FLConstants;

/**
 * Waterlogging renderer hook
 *
 * Injection point:
 *
 * <pre>
 * {@code
 *    <------------ here
 *  int k3 = block.getRenderBlockPass();
 *
 *  if (k3 > k2)
 *  {
 *      flag = true;
 *  }
 *
 *  if (!block.canRenderInPass(k2)) continue;
 * }
 * </pre>
 *
 * Injected code snippet:
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     int tmp = ASMHooks.drawFluidLogged(renderblocks, x, y, z, pass);
 *     nextPass |= tmp & 1;
 *     renderedAnything |= (tmp >>> 1) & 1;
 * }
 * </pre>
 */
public class FluidLogRendererInjector implements TurboClassTransformer {

    private static final Logger LOGGER = LogManager.getLogger(FLConstants.MOD_NAME);
    private static final String TARGET_CLASS = "net.minecraft.client.renderer.WorldRenderer";
    private static final String TARGET_METHOD = "updateRenderer";
    private static final String TARGET_METHOD_DESCRIPTOR = "(Lnet/minecraft/entity/EntityLivingBase;)V";
    private static final String HOOK_OWNER = FLConstants.ROOT_PKG.replace('.', '/') + "/internal/core/ASMHooks";
    private static final String HOOK_NAME = "drawFluidLogged";
    private static final String HOOK_DESCRIPTOR = "(Lnet/minecraft/client/renderer/RenderBlocks;IIII)I";
    private static final int REQUIRED_MAX_LOCALS = 24;

    @Override
    public String owner() {
        return FLConstants.MOD_ID;
    }

    @Override
    public String name() {
        return "FluidLogRendererInjector";
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
        val type = ASMUtil.discoverClassMappingType(cn);
        val block = MappingManager.classForName(NameType.Regular, MappingType.MCP, "net.minecraft.block.Block");
        val blockMethod = block.getMethod(MappingType.MCP, "getRenderBlockPass", "()I");
        val blockClassNameInternal = block.internalName()
            .get(type);
        val blockMethodName = blockMethod.name()
            .get(type);
        val blockMethodDesc = blockMethod.descriptor()
            .get(type);
        val method = ASMUtil.findMethodFromMCP(cn, TARGET_METHOD, TARGET_METHOD_DESCRIPTOR, false);
        if (method == null) {
            LOGGER
                .warn("Unable to locate {}.{}; FluidLogged renderer hook was not applied", TARGET_CLASS, TARGET_METHOD);
            return false;
        }
        if (method.maxLocals < REQUIRED_MAX_LOCALS) {
            LOGGER.warn(
                "Unsupported {} local layout (maxLocals={}); FluidLogged renderer hook was not applied",
                TARGET_METHOD,
                method.maxLocals);
            return false;
        }
        if (hasHook(method)) {
            return true;
        }

        MethodInsnNode target = null;
        for (val instruction : method.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) {
                continue;
            }
            val methodCall = (MethodInsnNode) instruction;
            if (!blockClassNameInternal.equals(methodCall.owner) || !blockMethodName.equals(methodCall.name)
                || !blockMethodDesc.equals(methodCall.desc)) {
                continue;
            }
            if (target != null) {
                LOGGER.warn(
                    "Ambiguous {} getRenderBlockPass call; FluidLogged renderer hook was not applied",
                    TARGET_METHOD);
                return false;
            }
            target = methodCall;
        }
        if (target == null) {
            LOGGER.warn("{} has no getRenderBlockPass call; FluidLogged renderer hook was not applied", TARGET_METHOD);
            return false;
        }
        int callIndex = method.instructions.indexOf(target);
        if (!validateLocals(method, callIndex)) {
            LOGGER.warn(
                "Unsupported {} local layout; FluidLogged renderer hook was not applied (this can happen with renderer transformers such as OptiFine)",
                TARGET_METHOD);
            return false;
        }

        val replacement = new InsnList();
        replacement.add(new VarInsnNode(Opcodes.ALOAD, 16));
        replacement.add(new VarInsnNode(Opcodes.ILOAD, 23));
        replacement.add(new VarInsnNode(Opcodes.ILOAD, 21));
        replacement.add(new VarInsnNode(Opcodes.ILOAD, 22));
        replacement.add(new VarInsnNode(Opcodes.ILOAD, 17));
        replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER, HOOK_NAME, HOOK_DESCRIPTOR, false));
        replacement.add(new InsnNode(Opcodes.DUP));
        replacement.add(new InsnNode(Opcodes.ICONST_1));
        replacement.add(new InsnNode(Opcodes.IAND));
        replacement.add(new VarInsnNode(Opcodes.ILOAD, 18));
        replacement.add(new InsnNode(Opcodes.IOR));
        replacement.add(new VarInsnNode(Opcodes.ISTORE, 18));
        replacement.add(new InsnNode(Opcodes.ICONST_1));
        replacement.add(new InsnNode(Opcodes.IUSHR));
        replacement.add(new InsnNode(Opcodes.ICONST_1));
        replacement.add(new InsnNode(Opcodes.IAND));
        replacement.add(new VarInsnNode(Opcodes.ILOAD, 19));
        replacement.add(new InsnNode(Opcodes.IOR));
        replacement.add(new VarInsnNode(Opcodes.ISTORE, 19));
        method.instructions.insertBefore(target, replacement);
        return true;
    }

    private static boolean hasHook(MethodNode method) {
        for (val instruction : method.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) {
                continue;
            }
            val call = (MethodInsnNode) instruction;
            if (HOOK_OWNER.equals(call.owner) && HOOK_NAME.equals(call.name) && HOOK_DESCRIPTOR.equals(call.desc)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Development jars keep the full local variable table, production jars usually only keep the method arguments.
     * Validate the hardcoded slots when the table is complete, but keep the legacy behaviour when it is not.
     */
    private static boolean validateLocals(MethodNode method, int callIndex) {
        if (method.localVariables == null || method.localVariables.isEmpty()) {
            return true;
        }
        boolean hasNonArgumentLocals = false;
        for (val local : method.localVariables) {
            if (local.index >= 2) {
                hasNonArgumentLocals = true;
                break;
            }
        }
        if (!hasNonArgumentLocals) {
            LOGGER.debug("{} only has argument locals; skipping local layout validation", TARGET_METHOD);
            return true;
        }
        return hasLocal(method, callIndex, 16, "Lnet/minecraft/client/renderer/RenderBlocks;")
            && hasLocal(method, callIndex, 17, "I")
            && hasFlagLocal(method, callIndex, 18)
            && hasFlagLocal(method, callIndex, 19)
            && hasLocal(method, callIndex, 21, "I")
            && hasLocal(method, callIndex, 22, "I")
            && hasLocal(method, callIndex, 23, "I");
    }

    private static boolean hasFlagLocal(MethodNode method, int callIndex, int index) {
        return hasLocal(method, callIndex, index, "Z") || hasLocal(method, callIndex, index, "I");
    }

    private static boolean hasLocal(MethodNode method, int callIndex, int index, String descriptor) {
        for (val local : method.localVariables) {
            if (local.index != index || !descriptor.equals(local.desc)) {
                continue;
            }
            int start = method.instructions.indexOf(local.start);
            int end = method.instructions.indexOf(local.end);
            if (start >= 0 && end >= 0 && callIndex >= start && callIndex < end) {
                return true;
            }
        }
        return false;
    }
}
