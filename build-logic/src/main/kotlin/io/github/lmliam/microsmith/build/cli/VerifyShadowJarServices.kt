package io.github.lmliam.microsmith.build.cli

import java.nio.charset.StandardCharsets
import java.util.jar.JarFile
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class VerifyShadowJarServices : CliBundledPluginTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val shadowJarFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val catalogFile: RegularFileProperty

    @get:Input
    abstract val catalogJarPath: Property<String>

    @TaskAction
    fun verify() {
        val plugins = bundledPlugins()
        val serviceEntries = CliBundledPluginProfile.serviceEntries(plugins)
        val coordinates = plugins.map { (coordinate) -> coordinate }
        val cliVersionValue = cliVersion.get()
        val formatVersionValue = formatVersion.get()

        val generatedCatalogFile = catalogFile.get().asFile
        val expectedCatalogText = generatedCatalogFile.readText(StandardCharsets.UTF_8)
        CliBundledPluginCatalogVerifier.validate(
            expectedCatalogText,
            generatedCatalogFile.path,
            formatVersionValue,
            cliVersionValue,
            coordinates,
            serviceEntries,
        )

        val catalogJarEntry = catalogJarPath.get()
        val shadedJar = shadowJarFile.get().asFile
        val expectedProvidersByDescriptor =
            serviceEntries.groupBy { entry -> entry.servicePath }.mapValues { (_, entries) ->
                entries.map { entry -> entry.provider }
            }

        JarFile(shadedJar).use { jarFile ->
            expectedProvidersByDescriptor.forEach { (servicePath, expectedProviders) ->
                val entry =
                    jarFile.getJarEntry(servicePath)
                        ?: throw GradleException(
                            "Missing merged service descriptor '$servicePath' in ${shadedJar.name}.",
                        )
                val providers =
                    jarFile.getInputStream(entry)
                        .bufferedReader(StandardCharsets.UTF_8)
                        .use { reader ->
                            reader.lineSequence()
                                .map(String::trim)
                                .filter { line -> line.isNotEmpty() && !line.startsWith("#") }
                                .toList()
                        }
                expectedProviders.forEach { expectedProvider ->
                    if (expectedProvider !in providers) {
                        throw GradleException(
                            "Service '$servicePath' is missing expected provider '$expectedProvider' " +
                                "in ${shadedJar.name}.",
                        )
                    }
                }
            }

            val catalogEntry =
                jarFile.getJarEntry(catalogJarEntry)
                    ?: throw GradleException(
                        "Missing bundled plugin catalog '$catalogJarEntry' in ${shadedJar.name}.",
                    )
            val actualCatalogText =
                jarFile.getInputStream(catalogEntry)
                    .bufferedReader(StandardCharsets.UTF_8)
                    .use { reader -> reader.readText() }
            if (actualCatalogText != expectedCatalogText) {
                throw GradleException(
                    "Bundled plugin catalog in ${shadedJar.name} does not match generated metadata.",
                )
            }
            CliBundledPluginCatalogVerifier.validate(
                actualCatalogText,
                "${shadedJar.name}!/$catalogJarEntry",
                formatVersionValue,
                cliVersionValue,
                coordinates,
                serviceEntries,
            )
        }
    }
}
