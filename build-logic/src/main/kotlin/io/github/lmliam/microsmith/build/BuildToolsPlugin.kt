package io.github.lmliam.microsmith.build

import org.gradle.api.Plugin
import org.gradle.api.Project

class BuildToolsPlugin : Plugin<Project> {
    // Gradle adds an included build to the classpath only through an applied plugin.
    override fun apply(project: Project) = Unit
}
