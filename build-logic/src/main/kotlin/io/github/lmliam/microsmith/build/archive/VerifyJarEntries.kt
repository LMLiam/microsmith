package io.github.lmliam.microsmith.build.archive

import java.util.jar.JarFile
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Fails the build when a JAR does not contain each expected entry. */
abstract class VerifyJarEntries : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val archive: RegularFileProperty

    @get:Input
    abstract val expectedEntries: ListProperty<String>

    @get:Input
    abstract val artifactDescription: Property<String>

    @TaskAction
    fun verify() {
        val archiveFile = archive.get().asFile
        val description = artifactDescription.get()
        if (!archiveFile.isFile) {
            throw GradleException("$description '${archiveFile.path}' was not created.")
        }

        val entryNames = JarFile(archiveFile).use { jarFile -> jarFile.entries().asSequence().map { it.name }.toSet() }
        val missingEntries = expectedEntries.get().filterNot(entryNames::contains)
        if (missingEntries.isNotEmpty()) {
            throw GradleException(
                "$description '${archiveFile.name}' is missing expected entries: ${missingEntries.joinToString(
                    ", ",
                )}",
            )
        }
    }
}
