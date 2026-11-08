package io.github.lmliam.microsmith.build

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Places the Microsmith build task classes on the build classpath.
 *
 * The CLI and runtime-scripting modules register these task types from their own build
 * scripts. Gradle adds an included build to the build classpath only when a build script
 * applies one of its plugins, so this plugin exists for that purpose alone. It registers
 * no tasks and changes no project configuration.
 *
 * The root build applies this plugin. Subproject build scripts inherit the root build
 * classpath, so every module can import the task types.
 */
class BuildToolsPlugin : Plugin<Project> {
    override fun apply(project: Project) = Unit
}
