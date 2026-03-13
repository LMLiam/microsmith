plugins {
    kotlin("jvm") version "2.2.21"
    id("me.liam.microsmith.gradle")
}

kotlin {
    jvmToolchain(21)
}

microsmithGradle {
    scriptFile.set(layout.projectDirectory.file("schema.microsmith.kts"))
}

tasks.named("check") {
    dependsOn(tasks.named("microsmithGenerate"))
}
