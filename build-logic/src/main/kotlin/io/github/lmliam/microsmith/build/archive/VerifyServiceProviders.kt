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

abstract class VerifyServiceProviders : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val archive: RegularFileProperty

    @get:Input
    abstract val expectedProviders: ListProperty<String>

    @get:Input
    abstract val artifactDescription: Property<String>

    @TaskAction
    fun verify() {
        val archiveFile = archive.get().asFile
        val description = artifactDescription.get()
        if (!archiveFile.isFile) {
            throw GradleException("$description '${archiveFile.path}' was not created.")
        }

        val requiredProviders =
            expectedProviders.get().map { declaration ->
                val separatorIndex = declaration.indexOf('=')
                if (separatorIndex < 0) {
                    throw GradleException(
                        "Invalid expected provider '$declaration' for $description. " +
                            "Expected '<service-descriptor>=<provider-class>'.",
                    )
                }
                declaration.substring(0, separatorIndex) to declaration.substring(separatorIndex + 1)
            }

        JarFile(archiveFile).use { jarFile ->
            requiredProviders
                .groupBy({ (servicePath) -> servicePath }, { (_, provider) -> provider })
                .forEach { (servicePath, expected) ->
                    val entry =
                        jarFile.getJarEntry(servicePath)
                            ?: throw GradleException(
                                "$description '${archiveFile.name}' is missing service descriptor '$servicePath'.",
                            )
                    val providers =
                        jarFile.getInputStream(entry)
                            .bufferedReader(Charsets.UTF_8)
                            .use { reader ->
                                reader.lineSequence()
                                    .map(String::trim)
                                    .filter { line -> line.isNotEmpty() && !line.startsWith("#") }
                                    .toSet()
                            }
                    expected.forEach { expectedProvider ->
                        if (expectedProvider !in providers) {
                            throw GradleException(
                                "$description '${archiveFile.name}' is missing provider " +
                                    "'$expectedProvider' in '$servicePath'.",
                            )
                        }
                    }
                }
        }
    }
}
