package me.liam.microsmith.gradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass") // Gradle instantiates managed extension properties through an abstract type.
abstract class MicrosmithGradleExtension
@Inject
constructor() {
    abstract val scriptFile: RegularFileProperty
    abstract val outputDirectory: DirectoryProperty
    abstract val variables: MapProperty<String, String>
    abstract val flags: SetProperty<String>
}
