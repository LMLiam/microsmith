package io.github.lmliam.microsmith.build.layers

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class VerifyModuleLayers : DefaultTask() {
    @get:Input
    abstract val projectDependencies: MapProperty<String, List<String>>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val abiDumps: ConfigurableFileCollection

    init {
        group = "verification"
        description = "Verifies pipeline layer direction between modules and that no published API exposes a Metro type."
    }

    @TaskAction
    fun verify() {
        val violations =
            findModuleLayerViolations(
                projectDependencies.get(),
                abiDumps.files.associate { it.nameWithoutExtension to it.readLines() },
            )
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Module layering violations:\n" + violations.joinToString("\n") { "  ${it.describe()}" },
            )
        }
    }
}

private fun ModuleLayerViolation.describe(): String =
    when (this) {
        is ModuleLayerViolation.UpwardDependency ->
            "$module (${moduleLayer.moduleName}) depends upward on $dependency (${dependencyLayer.moduleName})"
        is ModuleLayerViolation.PluginApiDependsOnRuntimeScripting ->
            "$module must not depend on the runtime scripting module $dependency"
        is ModuleLayerViolation.PluginDependsOnGen -> "$module must not depend on the gen module $dependency"
        is ModuleLayerViolation.MetroTypeInPublishedApi ->
            "$module exposes the Metro type ${type.internalName} in its published API"
    }
