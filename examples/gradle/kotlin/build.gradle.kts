plugins {
    kotlin("jvm") version "2.4.21"
    id("io.github.lmliam.microsmith")
}

kotlin {
    jvmToolchain(25)
}

microsmith {
    scriptFile.set(layout.projectDirectory.file("schema.microsmith.kts"))
}

tasks.named("check") {
    dependsOn(tasks.named("microsmithGenerate"))
}
