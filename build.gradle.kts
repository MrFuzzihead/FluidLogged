plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

// The convention plugin keeps the Minecraft/Forge classes on the main compile
// classpath as file dependencies, which the test source set does not inherit.
// Add the main compile classpath so tests can inspect real class bytes and use
// the mod's Minecraft-facing helpers.
val mainCompileClasspath = sourceSets.main.get().compileClasspath

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.ow2.asm:asm:9.10.1")
    testImplementation("org.ow2.asm:asm-tree:9.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
    testImplementation(mainCompileClasspath.filter { !it.name.contains("asm-debug-all") })
}

tasks.test {
    useJUnitPlatform()
    systemProperty(
        "fluidlogged.test.recompiledMinecraft",
        layout.buildDirectory.file("rfg/recompiled_minecraft-1.7.10.jar").get().asFile.absolutePath,
    )
    systemProperty(
        "fluidlogged.test.srgMinecraft",
        layout.buildDirectory.file("rfg/srg_merged_minecraft.jar").get().asFile.absolutePath,
    )
}

// The test harness reads modern class files (Angelica is compiled above Java 8).
// MC's launchwrapper pulls in asm-debug-all 5.0.3, which shadows the ASM 9 jars we
// use for class parsing. Keep the test classpaths on ASM 9.
configurations.named("testCompileClasspath") {
    exclude(group = "org.ow2.asm", module = "asm-debug-all")
}
configurations.named("testRuntimeClasspath") {
    exclude(group = "org.ow2.asm", module = "asm-debug-all")
}
