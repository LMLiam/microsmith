package io.github.lmliam.microsmith.build.archive

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

abstract class VerifyServiceProviders : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val archive: RegularFileProperty

    @get:Input
    abstract val expectedProviders: ListProperty<String>

    @get:Input
    @get:Optional
    abstract val artifactDescription: Property<String>

    init {
        group = "verification"
        description = "Verifies SPI service providers in the target JAR archive."
        artifactDescription.convention("Archive")
    }

    @TaskAction
    fun verify() {
        val archiveFile = archive.asFile.get()
        val description = artifactDescription.get()
        if (!archiveFile.isFile) {
            throw GradleException("$description '${archiveFile.path}' was not created.")
        }

        val parsedExpectations = expectedProviders.get().map { parseDeclaration(it, description) }
        val providersByService = parsedExpectations.groupBy({ it.first }, { it.second })

        JarFile(archiveFile).use { jar ->
            providersByService.forEach { (servicePath, expected) ->
                val actualProviders =
                    jar.readServiceProviders(servicePath)
                        ?: throw GradleException(
                            "$description '${archiveFile.name}' is missing service descriptor '$servicePath'.",
                        )

                val missing = expected.filterNot(actualProviders::contains)
                if (missing.isNotEmpty()) {
                    val missingList = missing.joinToString(", ")
                    throw GradleException(
                        "$description '${archiveFile.name}' is missing provider(s) '$missingList' in '$servicePath'.",
                    )
                }
            }
        }
    }

    private fun parseDeclaration(
        declaration: String,
        description: String,
    ): Pair<String, String> {
        val parts = declaration.split('=', limit = 2)
        if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw GradleException(
                "Invalid expected provider '$declaration' for $description. " +
                    "Expected '<service-descriptor>=<provider-class>'.",
            )
        }
        return parts[0].trim() to parts[1].trim()
    }

    private fun JarFile.readServiceProviders(servicePath: String): Set<String>? {
        val entry = getJarEntry(servicePath) ?: return null
        return getInputStream(entry).bufferedReader(Charsets.UTF_8).use { reader ->
            reader
                .lineSequence()
                .map { it.substringBefore('#').trim() }
                .filter { it.isNotEmpty() }
                .toSet()
        }
    }
}
