package io.github.lmliam.microsmith.build.cli

import java.io.File
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

    init {
        group = "verification"
        description = "Verifies merged SPI service providers and embedded catalog metadata in the Shadow JAR."
    }

    @TaskAction
    fun verify() {
        val plugins = bundledPlugins()
        val serviceEntries = CliBundledPluginProfile.serviceEntries(plugins)
        val coordinates = plugins.map { it.first }
        val currentCliVersion = cliVersion.get()
        val currentFormatVersion = formatVersion.get()

        val generatedCatalog = catalogFile.asFile.get()
        val expectedCatalogText = generatedCatalog.readText()
        CliBundledPluginCatalogVerifier.validate(
            catalogText = expectedCatalogText,
            source = generatedCatalog.path,
            expectedFormatVersion = currentFormatVersion,
            expectedCliVersion = currentCliVersion,
            expectedCoordinates = coordinates,
            expectedServiceEntries = serviceEntries,
        )

        val shadowJar = shadowJarFile.asFile.get()
        val catalogEntryPath = catalogJarPath.get()
        val expectedProvidersByService = serviceEntries.groupBy({ it.servicePath }, { it.provider })

        JarFile(shadowJar).use { jar ->
            jar.verifyServiceProviders(shadowJar, expectedProvidersByService)
            jar.verifyEmbeddedCatalog(
                shadowJar = shadowJar,
                catalogEntryPath = catalogEntryPath,
                expectedCatalogText = expectedCatalogText,
                formatVersion = currentFormatVersion,
                cliVersion = currentCliVersion,
                coordinates = coordinates,
                serviceEntries = serviceEntries,
            )
        }
    }

    private fun JarFile.verifyServiceProviders(
        jarFile: File,
        expectedProvidersByService: Map<String, List<String>>,
    ) {
        expectedProvidersByService.forEach { (servicePath, expectedProviders) ->
            val actualProviders =
                readServiceProviders(servicePath)
                    ?: throw GradleException("Missing merged service descriptor '$servicePath' in '${jarFile.name}'.")

            val missing = expectedProviders.filterNot(actualProviders::contains)
            if (missing.isNotEmpty()) {
                val formattedMissing = missing.joinToString(", ")
                throw GradleException(
                    "Service '$servicePath' is missing expected provider(s) '$formattedMissing' in '${jarFile.name}'.",
                )
            }
        }
    }

    private fun JarFile.verifyEmbeddedCatalog(
        shadowJar: File,
        catalogEntryPath: String,
        expectedCatalogText: String,
        formatVersion: Int,
        cliVersion: String,
        coordinates: List<String>,
        serviceEntries: List<BundledServiceEntry>,
    ) {
        val actualCatalogText =
            readEntryText(catalogEntryPath)
                ?: throw GradleException("Missing bundled plugin catalog '$catalogEntryPath' in '${shadowJar.name}'.")

        if (actualCatalogText != expectedCatalogText) {
            throw GradleException("Bundled plugin catalog in '${shadowJar.name}' does not match generated metadata.")
        }

        CliBundledPluginCatalogVerifier.validate(
            catalogText = actualCatalogText,
            source = "${shadowJar.name}!/$catalogEntryPath",
            expectedFormatVersion = formatVersion,
            expectedCliVersion = cliVersion,
            expectedCoordinates = coordinates,
            expectedServiceEntries = serviceEntries,
        )
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

    private fun JarFile.readEntryText(entryPath: String): String? {
        val entry = getJarEntry(entryPath) ?: return null
        return getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}
