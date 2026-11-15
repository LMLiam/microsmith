plugins {
    java
    id("io.github.lmliam.microsmith")
}

tasks.register("verifyMicrosmithWiring") {
    doLast {
        val generateTask = tasks.named("microsmithGenerate").get()
        check(generateTask is io.github.lmliam.microsmith.gradle.task.MicrosmithGenerateTask)
        check(generateTask.group == "microsmith")

        val ide = project.configurations.getByName("microsmithIde")
        val runtime = project.configurations.getByName("microsmithRuntime")
        val plugins = project.configurations.getByName("microsmithPlugins")
        val compileOnly = project.configurations.getByName("compileOnly")
        val extension = project.extensions.getByName("microsmith")

        check(ide.extendsFrom.contains(plugins))
        check(compileOnly.extendsFrom.contains(ide))
        check(!ide.isCanBeResolved)
        check(
            runtime.attributes.getAttribute(Bundling.BUNDLING_ATTRIBUTE)?.name ==
                Bundling.SHADOWED,
        )
        check(extension is io.github.lmliam.microsmith.gradle.MicrosmithGradleExtension)
    }
}
