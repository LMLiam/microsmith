package io.github.lmliam.microsmith.gradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.SetProperty

interface MicrosmithGradleExtension {
    val scriptFile: RegularFileProperty
    val outputDirectory: DirectoryProperty
    val variables: MapProperty<String, String>
    val flags: SetProperty<String>
}
