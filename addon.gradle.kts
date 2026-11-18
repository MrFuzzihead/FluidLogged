// Extra build configuration for the GTNH/RFG build.
//
// Unit tests and coverage use JUnit 5 + Mockito + JaCoCo. The in-game test mod lives in
// `src/gameTest/java` and is only added to a run classpath when `-PgameTest` or
// `gameTestServer` is used, so it never reaches the published jar.

import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import java.math.BigDecimal
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

// ---------------------------------------------------------------------------------------------------------
// Unit test dependencies
// ---------------------------------------------------------------------------------------------------------

dependencies {
    // JUnit 5 is already configured in build.gradle.kts. Mockito 5 needs Java 11+, which the
    // Gradle test JVM provides; the mod itself still targets Java 8.
    add("testImplementation", "org.mockito:mockito-core:5.24.0")
    add("testImplementation", "org.mockito:mockito-junit-jupiter:5.24.0")
}

// ---------------------------------------------------------------------------------------------------------
// JaCoCo
// ---------------------------------------------------------------------------------------------------------

apply(plugin = "jacoco")

project.extensions.configure(JacocoPluginExtension::class.java) {
    toolVersion = "0.8.16"
}

tasks.withType<Test>().configureEach {
    extensions.configure(JacocoTaskExtension::class.java) {
        isEnabled = true
    }
}

val mainSourceSet = project.extensions.getByType(SourceSetContainer::class.java).getByName("main")

// Mixins and the generated version holder are not meaningfully exercised headlessly. The contract
// tests inspect their bytecode, but that is not execution coverage.
val coverageClasses = project.files(mainSourceSet.output.classesDirs).asFileTree.matching {
    exclude("mega/fluidlogged/Tags.class", "mega/fluidlogged/internal/mixin/**")
}

tasks.named<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Generates HTML and XML JaCoCo coverage reports from the unit test run"
    dependsOn(tasks.named("test"))

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(coverageClasses)
    sourceDirectories.setFrom(mainSourceSet.allJava.srcDirs)
    executionData.setFrom(project.files(project.layout.buildDirectory.file("jacoco/test.exec")))
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    group = "verification"
    description = "Fails the build when unit test coverage drops below the current baseline"
    dependsOn(tasks.named("test"))

    classDirectories.setFrom(coverageClasses)
    sourceDirectories.setFrom(mainSourceSet.allJava.srcDirs)
    executionData.setFrom(project.files(project.layout.buildDirectory.file("jacoco/test.exec")))

    // Regression guard, not a target: the first measured baseline is ~20% lines / ~24% branches.
    // Raise these as tests are added instead of lowering them.
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.18")
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.20")
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------
// In-game test mod (never shipped)
// ---------------------------------------------------------------------------------------------------------

val gameTestSourceSet = project.extensions.getByType(SourceSetContainer::class.java).create("gameTest")

configurations.getByName(gameTestSourceSet.compileClasspathConfigurationName)
    .extendsFrom(configurations.getByName("compileClasspath"))
configurations.getByName(gameTestSourceSet.runtimeClasspathConfigurationName)
    .extendsFrom(configurations.getByName("runtimeClasspath"))
gameTestSourceSet.compileClasspath += mainSourceSet.output
gameTestSourceSet.compileClasspath += mainSourceSet.compileClasspath
gameTestSourceSet.runtimeClasspath += mainSourceSet.output
gameTestSourceSet.runtimeClasspath += mainSourceSet.runtimeClasspath
// The game-test mod is plain Java 8. Leaving Jabel's compiler plugin on this source set's classpath
// makes javac auto-load it through ServiceLoader, which then needs ByteBuddy's agent.
gameTestSourceSet.compileClasspath = gameTestSourceSet.compileClasspath.filter { !it.name.contains("jabel") }

val isGameTestRun = project.hasProperty("gameTest") ||
    gradle.startParameter.taskNames.any { it.contains("gameTestServer") }

if (isGameTestRun) {
    tasks.withType<JavaExec>().configureEach {
        if (name.startsWith("runServer") || name.startsWith("runClient")) {
            classpath += gameTestSourceSet.output
            systemProperty("fluidlogged.gameTest", "true")
            // A reused dev world may reference blocks from mods that are no longer installed; the game
            // test is not about that, so answer FML's "continue anyway?" prompt up front.
            systemProperty("fml.queryResult", "confirm")
        }
    }
}

tasks.named("check").configure {
    dependsOn(gameTestSourceSet.classesTaskName)
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}

tasks.register("gameTestServer") {
    group = "verification"
    description = "Boots a dedicated server, runs the in-game FluidLogged test mod, and exits with its result"
    if (isGameTestRun) {
        dependsOn(tasks.named("runServer"))
    }
}
