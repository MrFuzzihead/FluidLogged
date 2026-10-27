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

package mega.fluidlogged.internal.mixin.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Headless contract check between the Angelica renderer Mixin and the real Angelica 2.2.40 class. This catches
 * signature drift and annotation wiring mistakes without launching the game.
 */
class AngelicaMixinContractTest {

    private static final String TARGET = "com/gtnewhorizons/angelica/rendering/celeritas/AngelicaChunkBuilderMeshingTask";
    private static final String TARGET_RESOURCE = TARGET + ".class";
    private static final String MIXIN_RESOURCE = "mega/fluidlogged/internal/mixin/mixins/client/AngelicaRenderBlockMixin.class";

    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String SHADOW = "Lorg/spongepowered/asm/mixin/Shadow;";
    private static final String INJECT = "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String CALLBACK_INFO = "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;";

    @Test
    void rendererMixinMatchesTargetSignature() throws Exception {
        ClassNode mixin = parseResource(MIXIN_RESOURCE, "renderer mixin");
        ClassNode target = parseResource(TARGET_RESOURCE, "Angelica task");
        Assumptions.assumeTrue(mixin != null && target != null, "Angelica renderer mixin or target class not present");

        AnnotationNode mixinAnnotation = findAnnotation(classAnnotations(mixin), MIXIN);
        assertNotNull(mixinAnnotation, "renderer mixin is missing @Mixin");
        assertEquals(TARGET, firstType(mixinAnnotation, "value"), "renderer mixin targets the wrong class");

        MethodNode shadow = findMethodWithAnnotation(mixin, "renderBlock", SHADOW);
        assertNotNull(shadow, "renderer mixin is missing the renderBlock @Shadow");
        assertFalse(
            booleanValue(findAnnotation(methodAnnotations(shadow), SHADOW), "remap"),
            "renderBlock @Shadow must not be remapped");

        MethodNode targetRenderBlock = findMethod(target, "renderBlock");
        assertNotNull(targetRenderBlock, "Angelica task is missing renderBlock");
        assertEquals(targetRenderBlock.desc, shadow.desc, "renderBlock @Shadow descriptor drifted");

        MethodNode handler = findMethodWithAnnotation(mixin, "fluidlogged$renderFluid", INJECT);
        assertNotNull(handler, "renderer mixin is missing the @Inject handler");
        AnnotationNode inject = findAnnotation(methodAnnotations(handler), INJECT);
        assertEquals("renderBlock", firstString(inject, "method"), "@Inject targets the wrong method");
        assertFalse(booleanValue(inject, "remap"), "@Inject must not remap the Angelica method");
        assertEquals(0, intValue(inject, "require"), "@Inject should degrade gracefully when the target is absent");
        assertEquals(1, intValue(inject, "expect"), "@Inject should expect exactly one renderBlock RETURN");
        assertTrue(hasReturnAt(inject), "@Inject must use the renderBlock RETURN");

        String expectedHandlerDesc = withCallbackInfo(targetRenderBlock.desc);
        assertEquals(expectedHandlerDesc, handler.desc, "renderer handler descriptor does not match renderBlock");

    }

    @Test
    void targetExposesDeferredPipelineContract() throws Exception {
        ClassNode target = parseResource(TARGET_RESOURCE, "Angelica task");
        Assumptions.assumeTrue(target != null, "Angelica task not present");

        MethodNode prescan = findMethod(target, "prescanDeferred");
        assertNotNull(prescan, "Angelica 2.2.40 prescanDeferred is missing");
        assertEquals("(Lnet/minecraft/world/IBlockAccess;III[J)Z", prescan.desc, "prescanDeferred signature drifted");
        for (String local : new String[] { "region", "minX", "minY", "minZ", "x", "y", "z", "block" }) {
            assertHasLocal(prescan, "prescanDeferred", local);
        }

        MethodNode capture = findMethod(target, "captureDeferred");
        assertNotNull(capture, "Angelica 2.2.40 captureDeferred is missing");
        assertEquals(
            "(Lcom/gtnewhorizons/angelica/rendering/celeritas/DeferredSectionMesh;)"
                + "Lorg/embeddedt/embeddium/impl/render/chunk/compile/executor/ChunkJobResult;",
            capture.desc,
            "captureDeferred signature drifted");
    }

    private static String withCallbackInfo(String descriptor) {
        return descriptor.substring(0, descriptor.length() - 2) + CALLBACK_INFO
            + ")"
            + descriptor.charAt(descriptor.length() - 1);
    }

    private static void assertHasLocal(MethodNode method, String methodName, String name) {
        assertNotNull(method.localVariables, methodName + " has no local variable table");
        for (LocalVariableNode local : method.localVariables) {
            if (name.equals(local.name)) {
                return;
            }
        }
        throw new AssertionError(methodName + " is missing local '" + name + "'");
    }

    private static MethodNode findMethod(ClassNode cn, String name) {
        for (MethodNode method : cn.methods) {
            if (name.equals(method.name)) {
                return method;
            }
        }
        return null;
    }

    private static MethodNode findMethodWithAnnotation(ClassNode cn, String name, String annotation) {
        for (MethodNode method : cn.methods) {
            if (name != null && !name.equals(method.name)) {
                continue;
            }
            if (findAnnotation(methodAnnotations(method), annotation) != null) {
                return method;
            }
        }
        return null;
    }

    private static List<AnnotationNode> classAnnotations(ClassNode cn) {
        List<AnnotationNode> annotations = new ArrayList<>();
        if (cn.visibleAnnotations != null) annotations.addAll(cn.visibleAnnotations);
        if (cn.invisibleAnnotations != null) annotations.addAll(cn.invisibleAnnotations);
        return annotations;
    }

    private static List<AnnotationNode> methodAnnotations(MethodNode method) {
        List<AnnotationNode> annotations = new ArrayList<>();
        if (method.visibleAnnotations != null) annotations.addAll(method.visibleAnnotations);
        if (method.invisibleAnnotations != null) annotations.addAll(method.invisibleAnnotations);
        return annotations;
    }

    private static AnnotationNode findAnnotation(List<AnnotationNode> annotations, String descriptor) {
        if (annotations == null) return null;
        for (AnnotationNode annotation : annotations) {
            if (descriptor.equals(annotation.desc)) return annotation;
        }
        return null;
    }

    private static Object value(AnnotationNode annotation, String key) {
        if (annotation == null || annotation.values == null) return null;
        for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
            if (key.equals(annotation.values.get(i))) {
                return annotation.values.get(i + 1);
            }
        }
        return null;
    }

    private static String firstString(AnnotationNode annotation, String key) {
        Object value = value(annotation, key);
        if (value instanceof String) return (String) value;
        if (value instanceof List && !((List<?>) value).isEmpty()) {
            Object first = ((List<?>) value).get(0);
            return first instanceof String ? (String) first : null;
        }
        if (value instanceof String[] && ((String[]) value).length > 0) return ((String[]) value)[0];
        return null;
    }

    private static String firstType(AnnotationNode annotation, String key) {
        Object value = value(annotation, key);
        if (value instanceof Type) return ((Type) value).getInternalName();
        if (value instanceof List && !((List<?>) value).isEmpty()) {
            Object first = ((List<?>) value).get(0);
            return first instanceof Type ? ((Type) first).getInternalName() : null;
        }
        return null;
    }

    private static int intValue(AnnotationNode annotation, String key) {
        Object value = value(annotation, key);
        return value instanceof Integer ? (Integer) value : Integer.MIN_VALUE;
    }

    private static boolean booleanValue(AnnotationNode annotation, String key) {
        Object value = value(annotation, key);
        return !(value instanceof Boolean) || (Boolean) value;
    }

    private static boolean hasReturnAt(AnnotationNode inject) {
        Object at = value(inject, "at");
        if (!(at instanceof List)) return false;
        for (Object entry : (List<?>) at) {
            if (entry instanceof AnnotationNode && "RETURN".equals(firstString((AnnotationNode) entry, "value"))) {
                return true;
            }
        }
        return false;
    }

    private static ClassNode parseResource(String path, String description) throws Exception {
        try (InputStream in = AngelicaMixinContractTest.class.getClassLoader()
            .getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            byte[] bytes = readAll(in);
            if (bytes == null) {
                return null;
            }
            ClassNode cn = new ClassNode();
            new ClassReader(bytes).accept(cn, 0);
            return cn;
        }
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
}
