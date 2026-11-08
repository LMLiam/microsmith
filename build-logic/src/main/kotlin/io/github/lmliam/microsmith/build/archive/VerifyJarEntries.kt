package io.github.lmliam.microsmith.build.archive

import java.io.File
import java.util.jar.JarFile
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class VerifyJarEntries : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val archive: RegularFileProperty

    @get:Input
    abstract val expectedEntries: ListProperty<String>

    @get:Input
    @get:Optional
    abstract val artifactDescription: Property<String>

    init {
        group = "verification"
        description = "Verifies that the target JAR archive contains expected entry paths."
        artifactDescription.convention("Archive")
    }

    @TaskAction
    fun verify() {
        val archiveFile = archive.asFile.get()
        val description = artifactDescription.get()
        if (!archiveFile.isFile) {
            throw GradleException("$description '${archiveFile.path}' was not created.")
        }

        val missingEntries = expectedEntries.get().filterNot(archiveFile.readJarEntryNames()::contains)

        if (missingEntries.isNotEmpty()) {
            val formattedMissing = missingEntries.joinToString(", ")
            throw GradleException("$description '${archiveFile.name}' is missing expected entries: $formattedMissing")
        }
    }

    private fun File.readJarEntryNames(): Set<String> =
        JarFile(this).use { jar ->
            jar.entries().asSequence().mapTo(hashSetOf()) { it.name }
        }
}
