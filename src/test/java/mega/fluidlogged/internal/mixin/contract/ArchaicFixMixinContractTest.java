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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/** Headless wiring check for the optional ArchaicFix lighting mixin. */
class ArchaicFixMixinContractTest {

    private static final String MIXIN_RESOURCE = "mega/fluidlogged/internal/mixin/mixins/common/compat/archaicfix/MixinLightingEngine.class";
    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String PSEUDO = "Lorg/spongepowered/asm/mixin/Pseudo;";
    private static final String WRAP_OPERATION = "Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;";
    private static final String ARCHAFIX_ENGINE = "org.embeddedt.archaicfix.lighting.world.lighting.LightingEngine";

    @Test
    void targetsArchaicFixAsAPseudoMixin() throws Exception {
        ClassNode mixin = parseResource();
        assertNotNull(mixin, "ArchaicFix mixin class is missing");
        assertNotNull(findAnnotation(classAnnotations(mixin), PSEUDO), "ArchaicFix mixin must be @Pseudo");
        AnnotationNode target = findAnnotation(classAnnotations(mixin), MIXIN);
        assertNotNull(target, "ArchaicFix mixin is missing @Mixin");
        assertEquals(ARCHAFIX_ENGINE, firstString(target, "targets"));
    }

    @Test
    void wrapsOpacityAndLuminosity() throws Exception {
        ClassNode mixin = parseResource();
        assertNotNull(mixin, "ArchaicFix mixin class is missing");
        for (String methodName : new String[] { "fluidlogged$opacity", "fluidlogged$lightValue" }) {
            MethodNode method = findMethod(mixin, methodName);
            assertNotNull(method, "missing ArchaicFix handler " + methodName);
            AnnotationNode wrap = findAnnotation(methodAnnotations(method), WRAP_OPERATION);
            assertNotNull(wrap, methodName + " is missing @WrapOperation");
            String target = firstAtTarget(wrap);
            assertTrue(
                target != null && (target.contains("getLightOpacity") || target.contains("getLightValueForState")),
                methodName + " wraps an unexpected target");
        }
    }

    private static ClassNode parseResource() throws Exception {
        try (InputStream in = ArchaicFixMixinContractTest.class.getClassLoader()
            .getResourceAsStream(MIXIN_RESOURCE)) {
            if (in == null) return null;
            ClassNode cn = new ClassNode();
            new ClassReader(in).accept(cn, 0);
            return cn;
        }
    }

    private static MethodNode findMethod(ClassNode cn, String name) {
        for (MethodNode method : cn.methods) {
            if (name.equals(method.name)) return method;
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
        for (AnnotationNode annotation : annotations) {
            if (descriptor.equals(annotation.desc)) return annotation;
        }
        return null;
    }

    private static String firstAtTarget(AnnotationNode wrap) {
        if (wrap == null || wrap.values == null) return null;
        for (int i = 0; i + 1 < wrap.values.size(); i += 2) {
            if (!"at".equals(wrap.values.get(i))) continue;
            Object value = wrap.values.get(i + 1);
            if (value instanceof List && !((List<?>) value).isEmpty()) {
                Object first = ((List<?>) value).get(0);
                if (first instanceof AnnotationNode) return firstString((AnnotationNode) first, "target");
            }
        }
        return null;
    }

    private static String firstString(AnnotationNode annotation, String key) {
        if (annotation == null || annotation.values == null) return null;
        for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
            if (!key.equals(annotation.values.get(i))) continue;
            Object value = annotation.values.get(i + 1);
            if (value instanceof String) return (String) value;
            if (value instanceof List && !((List<?>) value).isEmpty()) {
                Object first = ((List<?>) value).get(0);
                if (first instanceof String) return (String) first;
            }
            if (value instanceof Type) return ((Type) value).getInternalName();
        }
        return null;
    }
}
