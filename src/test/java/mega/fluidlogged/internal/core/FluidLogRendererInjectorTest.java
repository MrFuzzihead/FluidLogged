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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Headless harness for the vanilla WorldRenderer transformer. It does not launch Minecraft: it parses the real
 * WorldRenderer class from the development and production-shaped jars and exercises the local-layout validation that
 * guards the hard-coded injection slots.
 */
class FluidLogRendererInjectorTest {

    private static final String TARGET_ENTRY = "net/minecraft/client/renderer/WorldRenderer.class";
    private static final String TARGET_DESCRIPTOR = "(Lnet/minecraft/entity/EntityLivingBase;)V";

    @Test
    void acceptsDevelopmentLocalLayout() throws Exception {
        Loaded loaded = load("fluidlogged.test.recompiledMinecraft", "updateRenderer", "getRenderBlockPass");
        assertTrue(validate(loaded));
    }

    @Test
    void acceptsProductionArgumentOnlyLocalLayout() throws Exception {
        Loaded loaded = load("fluidlogged.test.srgMinecraft", "func_147892_a", "func_149701_w");
        // Production jars usually only keep argument locals; the validator deliberately falls back in that case.
        assertTrue(validate(loaded));
    }

    @Test
    void rejectsCorruptedRenderBlocksSlot() throws Exception {
        Loaded loaded = load("fluidlogged.test.recompiledMinecraft", "updateRenderer", "getRenderBlockPass");
        boolean corrupted = false;
        for (LocalVariableNode local : loaded.method.localVariables) {
            if (local.index == 16 && "Lnet/minecraft/client/renderer/RenderBlocks;".equals(local.desc)) {
                local.desc = "Ljava/lang/Object;";
                corrupted = true;
            }
        }
        assertTrue(corrupted, "expected a RenderBlocks local at slot 16 in the development WorldRenderer");
        assertFalse(validate(loaded));
    }

    private static boolean validate(Loaded loaded) throws Exception {
        Method validate;
        try {
            validate = FluidLogRendererInjector.class.getDeclaredMethod("validateLocals", MethodNode.class, int.class);
        } catch (NoSuchMethodException e) {
            Assumptions.assumeTrue(false, "validateLocals is not present on this branch");
            return false;
        }
        validate.setAccessible(true);
        int callIndex = loaded.method.instructions.indexOf(loaded.call);
        return (Boolean) validate.invoke(null, loaded.method, callIndex);
    }

    private static Loaded load(String property, String methodName, String callName) throws Exception {
        String jarPath = System.getProperty(property);
        Assumptions.assumeTrue(jarPath != null, property + " is not configured");
        Path jar = Paths.get(jarPath);
        Assumptions.assumeTrue(Files.isRegularFile(jar), "missing jar: " + jar);

        ClassNode cn = new ClassNode();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            try (InputStream in = zip.getInputStream(zip.getEntry(TARGET_ENTRY))) {
                assertNotNull(in, "WorldRenderer.class not found in " + jar);
                new ClassReader(readAll(in)).accept(cn, 0);
            }
        }

        MethodNode method = null;
        for (MethodNode candidate : cn.methods) {
            if (methodName.equals(candidate.name) && TARGET_DESCRIPTOR.equals(candidate.desc)) {
                method = candidate;
                break;
            }
        }
        assertNotNull(method, "missing " + methodName + TARGET_DESCRIPTOR);

        MethodInsnNode call = null;
        for (org.objectweb.asm.tree.AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode candidate = (MethodInsnNode) instruction;
            if ("net/minecraft/block/Block".equals(candidate.owner) && callName.equals(candidate.name)
                && "()I".equals(candidate.desc)) {
                call = candidate;
                break;
            }
        }
        assertNotNull(call, "missing Block." + callName + "()I call");
        return new Loaded(method, call);
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static final class Loaded {

        private final MethodNode method;
        private final MethodInsnNode call;

        private Loaded(MethodNode method, MethodInsnNode call) {
            this.method = method;
            this.call = call;
        }
    }
}
