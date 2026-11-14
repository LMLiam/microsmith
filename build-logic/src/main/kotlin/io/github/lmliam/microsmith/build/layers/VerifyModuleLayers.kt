package io.github.lmliam.microsmith.build.layers

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class VerifyModuleLayers : DefaultTask() {
    @get:Input
    abstract val projectDependencies: MapProperty<String, List<String>>

    @get:Input
    abstract val metroModules: ListProperty<String>

    init {
        group = "verification"
        description = "Verifies pipeline layer direction between modules and that only gen applies Metro."
    }

    @TaskAction
    fun verify() {
        val violations = findModuleLayerViolations(projectDependencies.get(), metroModules.get())
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Module layering violations:\n" + violations.joinToString("\n") { "  $it" },
            )
        }
    }
}
