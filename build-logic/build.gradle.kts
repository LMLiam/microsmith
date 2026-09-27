import org.gradle.api.JavaVersion
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.shadow.gradle.plugin)
    compileOnly(gradleApi())
    testImplementation(gradleTestKit())
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

kotlin {
    jvmToolchain(24)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_22)
        progressiveMode.set(true)
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xexplicit-context-arguments",
        )
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_22
    targetCompatibility = JavaVersion.VERSION_22
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(22)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.named<Jar>("jar") {
    from(sourceSets.main.get().output)
}

gradlePlugin {
    plugins {
        register("repositoryQuality") {
            id = "io.github.lmliam.microsmith.repository-quality"
            implementationClass = "io.github.lmliam.microsmith.build.quality.RepositoryQualityPlugin"
            displayName = "Microsmith Repository Quality"
            description = "Registers repository structural Kotlin quality guardrails."
        }

        register("runtimeScripting") {
            id = "io.github.lmliam.microsmith.runtime-scripting"
            implementationClass = "io.github.lmliam.microsmith.build.runtime.RuntimeScriptingPlugin"
            displayName = "Microsmith Runtime Scripting"
            description = "Configures runtime scripting publication and IDE fallback artifacts."
        }

        register("cliPackaging") {
            id = "io.github.lmliam.microsmith.cli-packaging"
            implementationClass = "io.github.lmliam.microsmith.build.cli.CliPackagingPlugin"
            displayName = "Microsmith CLI Packaging"
            description = "Configures CLI packaging, distributions, and release artifacts."
        }

        register("mavenPluginDescriptor") {
            id = "io.github.lmliam.microsmith.maven-plugin"
            implementationClass = "io.github.lmliam.microsmith.build.maven.MavenPluginDescriptorPlugin"
            displayName = "Microsmith Maven Plugin Descriptor"
            description = "Configures Maven plugin descriptor generation and publication metadata."
        }
    }
}
