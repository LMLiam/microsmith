package io.github.lmliam.microsmith.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin

class MicrosmithGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create(
                MicrosmithGradleDsl.EXTENSION,
                MicrosmithGradleExtension::class.java,
            )
        extension.scriptFile.convention(project.layout.projectDirectory.file("build.microsmith.kts"))
        extension.outputDirectory.convention(project.layout.buildDirectory.dir("generated/microsmith"))
        extension.variables.convention(emptyMap())
        extension.flags.convention(emptySet())

        val microsmithPlugins =
            project.configurations.register(MicrosmithGradleConfigurations.PLUGINS) { configuration ->
                configuration.isCanBeConsumed = false
                configuration.isCanBeResolved = true
                configuration.isVisible = false
                configuration.description =
                    "Classpath for external Microsmith generator plugins used by Gradle tasks."
            }

        val microsmithRuntime =
            project.configurations.register(MicrosmithGradleConfigurations.RUNTIME) { configuration ->
                configuration.isCanBeConsumed = false
                configuration.isCanBeResolved = true
                configuration.isVisible = false
                configuration.description =
                    "Classpath for the isolated Microsmith worker JVM used by Gradle tasks."
                configuration.defaultDependencies { dependencies ->
                    dependencies.add(
                        project.dependencies.create(MicrosmithRuntimeDependencyNotation.runtimeScripting()),
                    )
                }
            }

        val microsmithIde =
            project.configurations.register(MicrosmithGradleConfigurations.IDE) { configuration ->
                configuration.isCanBeConsumed = false
                configuration.isCanBeResolved = false
                configuration.isVisible = false
                configuration.description =
                    "Microsmith script-definition and plugin classpath exposed to Gradle-imported IDEs."
                configuration.extendsFrom(microsmithRuntime.get())
                configuration.extendsFrom(microsmithPlugins.get())
            }

        project.plugins.withType(JavaBasePlugin::class.java) {
            project.configurations.named(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).configure {
                    configuration ->
                configuration.extendsFrom(microsmithIde.get())
            }
        }

        project.tasks.register(
            MicrosmithGradleTasks.GENERATE,
            MicrosmithGenerateTask::class.java,
        ) { task ->
            task.group = MicrosmithGradleTasks.GROUP
            task.description = "Generates artifacts from a .microsmith.kts script."
            task.scriptFile.convention(extension.scriptFile)
            task.outputDirectory.convention(extension.outputDirectory)
            task.variables.convention(extension.variables)
            task.flags.convention(extension.flags)
            task.pluginClasspath.from(microsmithPlugins)
            task.runtimeClasspath.from(
                project.files(task.javaClass.protectionDomain.codeSource.location),
                microsmithRuntime,
            )
            task.cacheDirectory.convention(project.layout.buildDirectory.dir("tmp/microsmith/cache"))
        }
    }
}
