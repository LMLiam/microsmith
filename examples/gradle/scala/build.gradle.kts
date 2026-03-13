plugins {
    scala
    id("me.liam.microsmith.gradle")
}

microsmithGradle {
    scriptFile.set(layout.projectDirectory.file("schema.microsmith.kts"))
}

tasks.named("check") {
    dependsOn(tasks.named("microsmithGenerate"))
}
