package io.github.lmliam.microsmith.build.archive

import java.util.jar.JarEntry
import java.util.jar.JarFile
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class VerifyServiceProviders : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val archive: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val bundledClasspath: ConfigurableFileCollection

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

        val providersByService = expectedProvidersByService()

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

    private fun expectedProvidersByService(): Map<String, Set<String>> {
        val providers = sortedMapOf<String, MutableSet<String>>()
        bundledClasspath.files.filter { it.isFile }.forEach { file ->
            JarFile(file).use { jar ->
                jar.entries().asSequence()
                    .filter { !it.isDirectory && it.name.startsWith(SERVICES_DIRECTORY) }
                    .filter { it.name.length > SERVICES_DIRECTORY.length }
                    .forEach { entry ->
                        providers.getOrPut(entry.name) { sortedSetOf() }.addAll(jar.readProviders(entry))
                    }
            }
        }
        return providers
    }

    private fun JarFile.readProviders(entry: JarEntry): Set<String> =
        getInputStream(entry).bufferedReader(Charsets.UTF_8).use { reader ->
            reader
                .lineSequence()
                .map { it.substringBefore('#').trim() }
                .filter { it.isNotEmpty() }
                .toSet()
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

private const val SERVICES_DIRECTORY = "META-INF/services/"
