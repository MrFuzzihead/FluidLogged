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
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Headless contract checks for the Angelica 2.2.40 deferred-block hooks. These catch target signature, injection point,
 * local-variable and accessor drift without starting the game.
 */
class AngelicaDeferredMixinContractTest {

    private static final String ANGELICA_TASK = "com/gtnewhorizons/angelica/rendering/celeritas/AngelicaChunkBuilderMeshingTask";
    private static final String DEFERRED_MESH = "com/gtnewhorizons/angelica/rendering/celeritas/DeferredSectionMesh";
    private static final String DEFERRED_BLOCK = "com/gtnewhorizons/angelica/rendering/celeritas/DeferredBlock";
    private static final String BUILD_CONTEXT = "com/gtnewhorizons/angelica/rendering/celeritas/AngelicaChunkBuildContext";
    private static final String WORLD_SLICE = "com/gtnewhorizons/angelica/rendering/celeritas/world/WorldSlice";
    private static final String CLONED_SECTION = "com/gtnewhorizons/angelica/rendering/celeritas/world/cloned/ClonedChunkSection";
    private static final String STATE_TESSELLATOR = "com/gtnewhorizons/angelica/rendering/StateAwareTessellator";
    private static final String SMOOTH_CACHE = "com/gtnewhorizons/angelica/rendering/celeritas/SmoothBiomeColorCache";
    private static final String RENDER_PASS_HELPER = "com/gtnewhorizons/angelica/rendering/celeritas/threading/RenderPassHelper";
    private static final String TESSELLATOR = "net/minecraft/client/renderer/Tessellator";

    private static final String MIXIN_RESOURCE = "mega/fluidlogged/internal/mixin/mixins/client/AngelicaChunkBuilderMeshingTaskMixin.class";
    private static final String ACCESSOR_RESOURCE = "mega/fluidlogged/internal/mixin/mixins/client/TessellatorAccessorMixin.class";

    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String SHADOW = "Lorg/spongepowered/asm/mixin/Shadow;";
    private static final String FINAL = "Lorg/spongepowered/asm/mixin/Final;";
    private static final String INJECT = "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String WRAP_OPERATION = "Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;";
    private static final String AT = "Lorg/spongepowered/asm/mixin/injection/At;";
    private static final String LOCAL = "Lcom/llamalad7/mixinextras/sugar/Local;";
    private static final String ACCESSOR = "Lorg/spongepowered/asm/mixin/gen/Accessor;";
    private static final String INVOKER = "Lorg/spongepowered/asm/mixin/gen/Invoker;";
    private static final String CALLBACK_INFO_RETURN = "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;";

    private static final String CAN_RENDER_OFF_THREAD = "L" + ANGELICA_TASK
        + ";canRenderOffThread(Lnet/minecraft/block/Block;)Z";

    @Test
    void deferredMixinTargetsTheAngelicaTask() throws Exception {
        ClassNode mixin = parseResource(MIXIN_RESOURCE, "deferred mixin");
        Assumptions.assumeTrue(mixin != null, "deferred mixin is not present");

        AnnotationNode mixinAnnotation = findAnnotation(classAnnotations(mixin), MIXIN);
        assertNotNull(mixinAnnotation, "deferred mixin is missing @Mixin");
        assertEquals(ANGELICA_TASK, firstType(mixinAnnotation, "value"), "deferred mixin targets the wrong class");
    }

    @Test
    void prescanWrapOperationMatchesAngelica() throws Exception {
        ClassNode mixin = parseResource(MIXIN_RESOURCE, "deferred mixin");
        ClassNode target = parseResource(ANGELICA_TASK + ".class", "Angelica task");
        Assumptions.assumeTrue(mixin != null && target != null, "deferred mixin or Angelica task not present");

        MethodNode handler = findMethodWithAnnotation(mixin, "fluidlogged$canRenderOffThread", WRAP_OPERATION);
        assertNotNull(handler, "deferred mixin is missing the prescan wrapper");
        AnnotationNode wrap = findAnnotation(methodAnnotations(handler), WRAP_OPERATION);
        assertNotNull(wrap, "prescan wrapper annotation missing");
        assertEquals("prescanDeferred", firstString(wrap, "method"), "wrapper targets the wrong method");
        assertAt(wrap, "INVOKE", CAN_RENDER_OFF_THREAD);
        assertEquals(0, intValue(wrap, "require"), "wrapper should degrade gracefully");
        assertEquals(1, intValue(wrap, "expect"), "wrapper should expect one canRenderOffThread call");
        assertFalse(booleanValue(wrap, "remap", true), "wrapper must not remap the Angelica method");

        MethodNode prescan = findMethod(target, "prescanDeferred");
        assertNotNull(prescan, "Angelica prescanDeferred is missing");
        assertEquals("(Lnet/minecraft/world/IBlockAccess;III[J)Z", prescan.desc, "prescanDeferred signature drifted");

        MethodNode canRenderOffThread = findMethod(target, "canRenderOffThread");
        assertNotNull(canRenderOffThread, "Angelica canRenderOffThread is missing");
        assertEquals("(Lnet/minecraft/block/Block;)Z", canRenderOffThread.desc, "canRenderOffThread signature drifted");
        assertEquals(
            0,
            canRenderOffThread.access & Opcodes.ACC_STATIC,
            "canRenderOffThread must remain an instance method for the wrapper call");
        assertTrue(
            hasInvoke(prescan, ANGELICA_TASK, "canRenderOffThread", "(Lnet/minecraft/block/Block;)Z"),
            "prescanDeferred no longer calls canRenderOffThread");

        String expectedHandlerDesc = "(L" + ANGELICA_TASK
            + ";Lnet/minecraft/block/Block;Lcom/llamalad7/mixinextras/injector/wrapoperation/Operation;"
            + "Lnet/minecraft/world/IBlockAccess;IIIIII)Z";
        assertEquals(expectedHandlerDesc, handler.desc, "prescan wrapper descriptor does not match the target");

        for (String local : new String[] { "region", "minX", "minY", "minZ", "x", "y", "z", "block" }) {
            assertHasLocal(prescan, "prescanDeferred", local);
        }

        assertLocalParameter(handler, 3, true, null, null);
        assertLocalParameter(handler, 4, true, 0, null);
        assertLocalParameter(handler, 5, true, 1, null);
        assertLocalParameter(handler, 6, true, 2, null);
        assertLocalParameter(handler, 7, false, null, "x");
        assertLocalParameter(handler, 8, false, null, "y");
        assertLocalParameter(handler, 9, false, null, "z");
        assertTrue(
            hasInvoke(
                handler,
                "mega/fluidlogged/internal/core/AngelicaSafetyHooks",
                "fluidCanRenderOffThread",
                "(Lnet/minecraft/world/IBlockAccess;III)Z"),
            "prescan wrapper no longer consults the fluid safety hook");
    }

    @Test
    void captureHandlerCallsDeferredHooks() throws Exception {
        ClassNode mixin = parseResource(MIXIN_RESOURCE, "deferred mixin");
        Assumptions.assumeTrue(mixin != null, "deferred mixin is not present");

        MethodNode handler = findMethodWithAnnotation(mixin, "fluidlogged$renderDeferredFluids", INJECT);
        assertNotNull(handler, "deferred mixin is missing the capture injection");

        assertTrue(
            hasInvoke(
                handler,
                "mega/fluidlogged/api/FLBlockAccess",
                "fl$getFluid",
                "(III)Lnet/minecraftforge/fluids/Fluid;"),
            "capture handler no longer reads the fluidlogged fluid");
        assertTrue(
            hasInvoke(
                handler,
                "mega/fluidlogged/internal/core/AngelicaSafetyHooks",
                "fluidRenderPass",
                "(Lnet/minecraft/block/Block;)I"),
            "capture handler no longer chooses a render pass");
        assertTrue(
            hasInvoke(
                handler,
                "mega/fluidlogged/internal/core/AngelicaSafetyHooks",
                "fluidMetadata",
                "(Lnet/minecraft/block/Block;)I"),
            "capture handler no longer records the fluid metadata");
        assertTrue(
            hasInvoke(
                handler,
                "mega/fluidlogged/internal/mixin/mixins/client/AngelicaChunkBuilderMeshingTaskMixin",
                "drawBlock",
                "(Lnet/minecraft/block/Block;IIIILnet/minecraft/client/renderer/Tessellator;"
                    + "Lnet/minecraft/client/renderer/RenderBlocks;)V"),
            "capture handler no longer draws the fluid on the main thread");
        assertTrue(
            hasInvoke(handler, DEFERRED_MESH + "$DeferredCapture", "add", "(I[II[I[II)V"),
            "capture handler no longer appends a deferred capture entry");
        assertTrue(
            hasInvoke(
                handler,
                DEFERRED_BLOCK,
                "<init>",
                "(IIILnet/minecraft/block/Block;IILorg/embeddedt/embeddium/impl/render/chunk/terrain/material/Material;)V"),
            "capture handler no longer appends a deferred block");
    }

    @Test
    void captureInjectionMatchesAngelica() throws Exception {
        ClassNode mixin = parseResource(MIXIN_RESOURCE, "deferred mixin");
        ClassNode target = parseResource(ANGELICA_TASK + ".class", "Angelica task");
        Assumptions.assumeTrue(mixin != null && target != null, "deferred mixin or Angelica task not present");

        MethodNode handler = findMethodWithAnnotation(mixin, "fluidlogged$renderDeferredFluids", INJECT);
        assertNotNull(handler, "deferred mixin is missing the capture injection");
        AnnotationNode inject = findAnnotation(methodAnnotations(handler), INJECT);
        assertNotNull(inject, "capture injection annotation missing");
        assertEquals("captureDeferred", firstString(inject, "method"), "capture injection targets the wrong method");
        assertAt(inject, "RETURN", null);
        assertEquals(0, intValue(inject, "require"), "capture injection should degrade gracefully");
        assertEquals(1, intValue(inject, "expect"), "capture injection should expect one RETURN");
        assertFalse(booleanValue(inject, "remap", true), "capture injection must not remap the Angelica method");

        MethodNode capture = findMethod(target, "captureDeferred");
        assertNotNull(capture, "Angelica captureDeferred is missing");
        assertEquals(
            "(L" + DEFERRED_MESH + ";)Lorg/embeddedt/embeddium/impl/render/chunk/compile/executor/ChunkJobResult;",
            capture.desc,
            "captureDeferred signature drifted");
        assertEquals(withCallbackInfoVoid(capture.desc), handler.desc, "capture handler descriptor does not match");
    }

    @Test
    void deferredShadowsMatchAngelica() throws Exception {
        ClassNode mixin = parseResource(MIXIN_RESOURCE, "deferred mixin");
        ClassNode target = parseResource(ANGELICA_TASK + ".class", "Angelica task");
        Assumptions.assumeTrue(mixin != null && target != null, "deferred mixin or Angelica task not present");

        FieldNode renderShadow = findField(mixin, "render");
        assertNotNull(renderShadow, "deferred mixin is missing the render shadow");
        AnnotationNode renderShadowAnnotation = findAnnotation(fieldAnnotations(renderShadow), SHADOW);
        assertNotNull(renderShadowAnnotation, "render field is missing @Shadow");
        assertFalse(booleanValue(renderShadowAnnotation, "remap", true), "render @Shadow must not remap");
        assertNotNull(findAnnotation(fieldAnnotations(renderShadow), FINAL), "render shadow must retain @Final");

        FieldNode renderTarget = findField(target, "render");
        assertNotNull(renderTarget, "Angelica task is missing the render field");
        assertEquals(renderTarget.desc, renderShadow.desc, "render @Shadow descriptor drifted");
        assertEquals(
            "Lorg/embeddedt/embeddium/impl/render/chunk/RenderSection;",
            renderTarget.desc,
            "Angelica render field type drifted");

        MethodNode drawShadow = findMethodWithAnnotation(mixin, "drawBlock", SHADOW);
        assertNotNull(drawShadow, "deferred mixin is missing the drawBlock shadow");
        AnnotationNode drawShadowAnnotation = findAnnotation(methodAnnotations(drawShadow), SHADOW);
        assertNotNull(drawShadowAnnotation, "drawBlock is missing @Shadow");
        assertFalse(booleanValue(drawShadowAnnotation, "remap", true), "drawBlock @Shadow must not remap");

        MethodNode drawTarget = findMethod(target, "drawBlock");
        assertNotNull(drawTarget, "Angelica task is missing drawBlock");
        assertEquals(drawTarget.desc, drawShadow.desc, "drawBlock @Shadow descriptor drifted");
        assertEquals(
            "(Lnet/minecraft/block/Block;IIIILnet/minecraft/client/renderer/Tessellator;"
                + "Lnet/minecraft/client/renderer/RenderBlocks;)V",
            drawTarget.desc,
            "Angelica drawBlock signature drifted");
        assertTrue(
            (drawTarget.access & Opcodes.ACC_PRIVATE) != 0,
            "Angelica drawBlock is expected to remain private for the @Shadow");
    }

    @Test
    void deferredCaptureDependenciesMatchAngelica() throws Exception {
        ClassNode mesh = parseResource(DEFERRED_MESH + ".class", "DeferredSectionMesh");
        ClassNode capture = parseResource(DEFERRED_MESH + "$DeferredCapture.class", "DeferredCapture");
        ClassNode block = parseResource(DEFERRED_BLOCK + ".class", "DeferredBlock");
        ClassNode buildContext = parseResource(BUILD_CONTEXT + ".class", "AngelicaChunkBuildContext");
        ClassNode stateTessellator = parseResource(STATE_TESSELLATOR + ".class", "StateAwareTessellator");
        ClassNode smoothCache = parseResource(SMOOTH_CACHE + ".class", "SmoothBiomeColorCache");
        ClassNode renderPassHelper = parseResource(RENDER_PASS_HELPER + ".class", "RenderPassHelper");
        Assumptions.assumeTrue(
            mesh != null && capture != null
                && block != null
                && buildContext != null
                && stateTessellator != null
                && smoothCache != null
                && renderPassHelper != null,
            "Angelica 2.2.40 deferred capture dependencies not present");

        assertField(mesh, "blocks", "Ljava/util/List;");
        assertField(mesh, "slice", "L" + WORLD_SLICE + ";");
        assertField(mesh, "capture", "L" + DEFERRED_MESH + "$DeferredCapture;");
        assertMethod(capture, "add", "(I[II[I[II)V");

        assertMethod(
            block,
            "<init>",
            "(IIILnet/minecraft/block/Block;IILorg/embeddedt/embeddium/impl/render/chunk/terrain/material/Material;)V");
        for (String accessor : new String[] { "x", "y", "z" }) {
            assertMethod(block, accessor, "()I");
        }
        assertMethod(block, "block", "()Lnet/minecraft/block/Block;");

        assertField(buildContext, "NUM_PASSES", "I");
        assertMethod(stateTessellator, "angelica$setCeleritasMeshing", "(Z)V");
        assertMethod(stateTessellator, "angelica$getVertexStates", "()[I");
        assertMethod(stateTessellator, "angelica$getShaderOverrideBlockIds", "()[I");
        assertMethod(smoothCache, "setActiveCache", "(L" + SMOOTH_CACHE + ";)V");
        assertMethod(smoothCache, "clearActiveCache", "()V");
        assertMethod(renderPassHelper, "resetWorldRenderPass", "()V");
    }

    @Test
    void angelicaWorldSliceExposesFluidLoggedAccess() throws Exception {
        ClassNode worldSlice = parseResource(WORLD_SLICE + ".class", "WorldSlice");
        ClassNode clonedSection = parseResource(CLONED_SECTION + ".class", "ClonedChunkSection");
        Assumptions.assumeTrue(worldSlice != null && clonedSection != null, "Angelica world slice classes not present");

        assertTrue(
            worldSlice.interfaces.contains("mega/fluidlogged/api/FLBlockAccess"),
            "WorldSlice must implement FLBlockAccess for deferred fluid capture");
        assertMethod(worldSlice, "fl$getFluid", "(III)Lnet/minecraftforge/fluids/Fluid;");
        assertMethod(worldSlice, "fl$setFluid", "(IIILnet/minecraftforge/fluids/Fluid;)V");
        assertField(worldSlice, "fluidArrays", "[[Lnet/minecraftforge/fluids/Fluid;");
        assertMethod(clonedSection, "getFluid", "(III)Lnet/minecraftforge/fluids/Fluid;");
    }

    @Test
    void tessellatorAccessorsMatchVanilla() throws Exception {
        ClassNode mixin = parseResource(ACCESSOR_RESOURCE, "Tessellator accessor mixin");
        ClassNode tessellator = parseResource(TESSELLATOR + ".class", "Tessellator");
        Assumptions
            .assumeTrue(mixin != null && tessellator != null, "Tessellator accessor mixin or target not present");

        AnnotationNode mixinAnnotation = findAnnotation(classAnnotations(mixin), MIXIN);
        assertNotNull(mixinAnnotation, "Tessellator accessor mixin is missing @Mixin");
        assertEquals(TESSELLATOR, firstType(mixinAnnotation, "value"), "accessor mixin targets the wrong class");

        MethodNode rawBuffer = findMethod(mixin, "fluidlogged$getRawBuffer");
        assertNotNull(rawBuffer, "rawBuffer accessor is missing");
        assertEquals("()[I", rawBuffer.desc, "rawBuffer accessor descriptor drifted");
        assertEquals("rawBuffer", accessorTarget(rawBuffer), "rawBuffer accessor targets the wrong field");

        MethodNode vertexCount = findMethod(mixin, "fluidlogged$getVertexCount");
        assertNotNull(vertexCount, "vertexCount accessor is missing");
        assertEquals("()I", vertexCount.desc, "vertexCount accessor descriptor drifted");
        assertEquals("vertexCount", accessorTarget(vertexCount), "vertexCount accessor targets the wrong field");

        MethodNode drawing = findMethod(mixin, "fluidlogged$setDrawing");
        assertNotNull(drawing, "isDrawing accessor is missing");
        assertEquals("(Z)V", drawing.desc, "isDrawing accessor descriptor drifted");
        assertEquals("isDrawing", accessorTarget(drawing), "isDrawing accessor targets the wrong field");

        MethodNode reset = findMethod(mixin, "fluidlogged$reset");
        assertNotNull(reset, "reset invoker is missing");
        assertEquals("()V", reset.desc, "reset invoker descriptor drifted");
        AnnotationNode invoker = findAnnotation(methodAnnotations(reset), INVOKER);
        assertNotNull(invoker, "reset method is missing @Invoker");
        assertEquals("reset", firstString(invoker, "value"), "reset invoker targets the wrong method");

        assertField(tessellator, "rawBuffer", "[I");
        assertField(tessellator, "vertexCount", "I");
        assertField(tessellator, "isDrawing", "Z");
        assertMethod(tessellator, "reset", "()V");
    }

    private static String accessorTarget(MethodNode method) {
        AnnotationNode accessor = findAnnotation(methodAnnotations(method), ACCESSOR);
        assertNotNull(accessor, method.name + " is missing @Accessor");
        return firstString(accessor, "value");
    }

    private static void assertLocalParameter(MethodNode method, int parameter, boolean argsOnly, Integer ordinal,
        String name) {
        AnnotationNode local = findParameterAnnotation(method, parameter, LOCAL);
        assertNotNull(local, "parameter " + parameter + " is missing @Local");
        assertEquals(argsOnly, booleanValue(local, "argsOnly", false), "parameter " + parameter + " argsOnly drifted");
        if (ordinal != null) {
            assertEquals(ordinal.intValue(), intValue(local, "ordinal"), "parameter " + parameter + " ordinal drifted");
        }
        if (name != null) {
            assertEquals(name, firstString(local, "name"), "parameter " + parameter + " local name drifted");
        }
    }

    private static void assertAt(AnnotationNode annotation, String value, String target) {
        AnnotationNode at = firstAt(annotation);
        assertNotNull(at, "missing @At on injection annotation");
        assertEquals(AT, at.desc, "@At annotation type drifted");
        assertEquals(value, firstString(at, "value"), "@At value drifted");
        if (target != null) {
            assertEquals(target, firstString(at, "target"), "@At target drifted");
        }
    }

    private static AnnotationNode firstAt(AnnotationNode annotation) {
        Object at = value(annotation, "at");
        if (at instanceof AnnotationNode) return (AnnotationNode) at;
        if (at instanceof List) {
            for (Object entry : (List<?>) at) {
                if (entry instanceof AnnotationNode) return (AnnotationNode) entry;
            }
        }
        return null;
    }

    private static String withCallbackInfoVoid(String descriptor) {
        int close = descriptor.lastIndexOf(')');
        return descriptor.substring(0, close) + CALLBACK_INFO_RETURN + ")V";
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

    private static boolean hasInvoke(MethodNode method, String owner, String name, String desc) {
        for (org.objectweb.asm.tree.AbstractInsnNode insn = method.instructions.getFirst(); insn
            != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) insn;
            if (owner.equals(call.owner) && name.equals(call.name) && desc.equals(call.desc)) {
                return true;
            }
        }
        return false;
    }

    private static void assertField(ClassNode cn, String name, String desc) {
        FieldNode field = findField(cn, name);
        assertNotNull(field, cn.name + " is missing field " + name);
        assertEquals(desc, field.desc, cn.name + "." + name + " descriptor drifted");
    }

    private static void assertMethod(ClassNode cn, String name, String desc) {
        assertNotNull(findMethod(cn, name, desc), cn.name + " is missing method " + name + desc);
    }

    private static MethodNode findMethod(ClassNode cn, String name) {
        return findMethod(cn, name, null);
    }

    private static MethodNode findMethod(ClassNode cn, String name, String desc) {
        for (MethodNode method : cn.methods) {
            if (name.equals(method.name) && (desc == null || desc.equals(method.desc))) {
                return method;
            }
        }
        return null;
    }

    private static FieldNode findField(ClassNode cn, String name) {
        for (FieldNode field : cn.fields) {
            if (name.equals(field.name)) {
                return field;
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

    private static List<AnnotationNode> fieldAnnotations(FieldNode field) {
        List<AnnotationNode> annotations = new ArrayList<>();
        if (field.visibleAnnotations != null) annotations.addAll(field.visibleAnnotations);
        if (field.invisibleAnnotations != null) annotations.addAll(field.invisibleAnnotations);
        return annotations;
    }

    private static AnnotationNode findParameterAnnotation(MethodNode method, int parameter, String descriptor) {
        AnnotationNode visible = findInParameterAnnotations(method.visibleParameterAnnotations, parameter, descriptor);
        if (visible != null) return visible;
        return findInParameterAnnotations(method.invisibleParameterAnnotations, parameter, descriptor);
    }

    private static AnnotationNode findInParameterAnnotations(List<AnnotationNode>[] annotations, int parameter,
        String descriptor) {
        if (annotations == null || parameter >= annotations.length) return null;
        return findAnnotation(annotations[parameter], descriptor);
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

    private static boolean booleanValue(AnnotationNode annotation, String key, boolean defaultValue) {
        Object value = value(annotation, key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    private static ClassNode parseResource(String path, String description) throws Exception {
        try (InputStream in = AngelicaDeferredMixinContractTest.class.getClassLoader()
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
