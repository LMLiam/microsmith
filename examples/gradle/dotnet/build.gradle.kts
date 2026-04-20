plugins {
    base
    id("io.github.lmliam.microsmith")
}

microsmith {
    outputDirectory.set(layout.projectDirectory.dir("Generated"))
}

tasks.named("check") {
    dependsOn(tasks.named("microsmithGenerate"))
}
