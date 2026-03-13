plugins {
    scala
    id("me.liam.microsmith.gradle")
}

microsmith {
    scriptFile.set(layout.projectDirectory.file("schema.microsmith.kts"))
}

tasks.named("check") {
    dependsOn(tasks.named("microsmithGenerate"))
}
