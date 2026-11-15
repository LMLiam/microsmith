plugins {
    id("io.github.lmliam.microsmith")
}

microsmith {
    scriptFile.set(layout.projectDirectory.file("schema.microsmith.kts"))
    outputDirectory.set(layout.projectDirectory.dir("custom-generated"))
    variables.put("entityName", "GradleConfiguredUserCreated")
    flags.add("emit")
}
