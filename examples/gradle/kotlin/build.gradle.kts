plugins {
    kotlin("jvm") version "2.2.21"
    id("io.github.lmliam.microsmith")
}

kotlin {
    jvmToolchain(21)
}

microsmith {
    scriptFile.set(layout.projectDirectory.file("schema.microsmith.kts"))
}

tasks.named("check") {
    dependsOn(tasks.named("microsmithGenerate"))
}
