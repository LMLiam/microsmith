import org.gradle.api.JavaVersion
import org.gradle.api.tasks.compile.JavaCompile
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
    testImplementation(gradleTestKit())
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

kotlin {
    jvmToolchain(25)

    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        progressiveMode.set(true)
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
        )
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        register("buildTools") {
            id = "io.github.lmliam.microsmith.build-tools"
            implementationClass = "io.github.lmliam.microsmith.build.BuildToolsPlugin"
            displayName = "Microsmith Build Tools"
            description = "Places the Microsmith build task types on the build classpath."
        }
    }
}
