package io.github.lmliam.microsmith.build.cli

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

abstract class CliBundledPluginTask : DefaultTask() {
    @get:Input
    abstract val cliVersion: Property<String>

    @get:Input
    abstract val formatVersion: Property<Int>

    @get:Input
    abstract val pluginCoordinates: ListProperty<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val pluginJars: ConfigurableFileCollection

    protected fun bundledPlugins(): List<Pair<String, File>> =
        CliBundledPluginProfile.plugins(pluginCoordinates.get(), pluginJars.files.toList())
}
