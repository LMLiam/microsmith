plugins {
    scala
    id("io.github.lmliam.microsmith")
}

microsmith {
    scriptFile.set(layout.projectDirectory.file("schema.microsmith.kts"))
}

tasks.named("check") {
    dependsOn(tasks.named("microsmithGenerate"))
}
