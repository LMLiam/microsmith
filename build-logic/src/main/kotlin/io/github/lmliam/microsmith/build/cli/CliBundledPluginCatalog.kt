package io.github.lmliam.microsmith.build.cli

import org.apache.tools.tar.TarInputStream
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.util.jar.JarFile
import java.util.zip.GZIPInputStream
import java.util.zip.ZipFile

internal data class BundledPluginCatalogEntry(
    val coordinate: String,
    val archiveFile: Provider<RegularFile>,
) {
    fun collectServiceEntries(): List<BundledServiceEntry> =
        CliBundledPluginCatalogVerifier.collectServiceEntries(archiveFile.get().asFile, coordinate)
}

internal data class BundledServiceEntry(
    val coordinate: String,
    val servicePath: String,
    val provider: String,
)

internal object CliBundledPluginCatalogVerifier {
    fun validate(
        catalogText: String,
        source: String,
        expectedFormatVersion: Int,
        expectedCliVersion: String,
        expectedCoordinates: List<String>,
        expectedServiceEntries: List<BundledServiceEntry>,
    ) {
        var formatValue: String? = null
        var cliVersion: String? = null
        val pluginCoordinates = mutableListOf<String>()
        val serviceEntries = mutableListOf<BundledServiceEntry>()

        catalogText.lineSequence().withIndex().forEach { indexedLine ->
            val index = indexedLine.index
            val line = indexedLine.value.trim()
            if (line.isEmpty() || line.startsWith("#")) {
                return@forEach
            }

            when {
                line.startsWith("format=") -> formatValue = line.removePrefix("format=")
                line.startsWith("cliVersion=") -> cliVersion = line.removePrefix("cliVersion=")
                line.startsWith("plugin=") -> pluginCoordinates += line.removePrefix("plugin=")
                line.startsWith("service=") -> {
                    val components = line.removePrefix("service=").split("|", limit = 3)
                    if (components.size != 3 || components.any { it.isEmpty() }) {
                        throw GradleException(
                            "Invalid service declaration at ${source}:${index + 1}. Expected " +
                                "'service=<coordinate>|<service-path>|<provider>'.",
                        )
                    }
                    serviceEntries += BundledServiceEntry(
                        coordinate = components[0],
                        servicePath = components[1],
                        provider = components[2],
                    )
                }
                else -> {
                    throw GradleException("Unrecognized bundled plugin catalog entry at ${source}:${index + 1}: '$line'")
                }
            }
        }

        if (formatValue != expectedFormatVersion.toString()) {
            throw GradleException(
                "Bundled plugin catalog '${source}' has format '${formatValue}', expected '${expectedFormatVersion}'.",
            )
        }

        if (cliVersion != expectedCliVersion) {
            throw GradleException(
                "Bundled plugin catalog '${source}' has cliVersion '${cliVersion}', expected '${expectedCliVersion}'.",
            )
        }

        if (pluginCoordinates != expectedCoordinates) {
            throw GradleException(
                "Bundled plugin catalog '${source}' defines coordinates ${pluginCoordinates}, " +
                    "expected ${expectedCoordinates}.",
            )
        }

        val sortedExpectedEntries = sortServiceEntries(expectedServiceEntries)
        val sortedActualEntries = sortServiceEntries(serviceEntries)
        if (sortedActualEntries != sortedExpectedEntries) {
            throw GradleException(
                "Bundled plugin catalog '${source}' service declarations do not match expected providers.",
            )
        }
    }

    fun collectServiceEntries(jarFile: File, coordinate: String): List<BundledServiceEntry> {
        val entries = mutableListOf<BundledServiceEntry>()
        JarFile(jarFile).use { jarFileHandle ->
            jarFileHandle.entries().asSequence()
                .filter { entry -> !entry.isDirectory && entry.name.startsWith("META-INF/services/") }
                .forEach { entry ->
                    val providers =
                        jarFileHandle.getInputStream(entry)
                            .bufferedReader(StandardCharsets.UTF_8)
                            .use { reader ->
                                reader.lineSequence()
                                    .map(String::trim)
                                    .filter { line -> line.isNotEmpty() && !line.startsWith("#") }
                                    .toList()
                            }

                    providers.forEach { provider ->
                        entries += BundledServiceEntry(
                            coordinate = coordinate,
                            servicePath = entry.name,
                            provider = provider,
                        )
                    }
                }
        }

        return sortServiceEntries(entries)
    }

    fun collectZipEntries(zipArchive: File): Set<String> {
        val entries = linkedSetOf<String>()
        ZipFile(zipArchive).use { zipFile ->
            zipFile.entries().asSequence()
                .filterNot { entry -> entry.isDirectory }
                .forEach { entry -> entries += entry.name }
        }
        return entries
    }

    fun collectTarGzEntries(tarGzArchive: File): Set<String> {
        val entries = linkedSetOf<String>()
        TarInputStream(GZIPInputStream(FileInputStream(tarGzArchive))).use { tarStream ->
            while (true) {
                val entry = tarStream.getNextEntry() ?: break
                if (!entry.isDirectory) {
                    entries += entry.name
                }
            }
        }
        return entries
    }

    private fun sortServiceEntries(entries: List<BundledServiceEntry>): List<BundledServiceEntry> =
        entries.sortedWith(
            compareBy<BundledServiceEntry> { it.coordinate }
                .thenBy { it.servicePath }
                .thenBy { it.provider },
        )
}

internal object CliBundledPluginCatalogWriter {
    fun buildText(
        cliVersion: String,
        formatVersion: Int,
        bundledPlugins: List<BundledPluginCatalogEntry>,
        bundledServiceEntries: List<BundledServiceEntry>,
    ): String =
        buildString {
            appendLine("# Microsmith bundled plugin profile")
            appendLine("# Plugin coordinates are pinned to the CLI release version for deterministic runtime behavior.")
            appendLine("format=$formatVersion")
            appendLine("cliVersion=$cliVersion")
            val serviceEntriesByCoordinate = bundledServiceEntries.groupBy { it.coordinate }
            bundledPlugins.forEach { plugin ->
                appendLine("plugin=${plugin.coordinate}")
                serviceEntriesByCoordinate.getOrDefault(plugin.coordinate, emptyList()).forEach { entry ->
                    appendLine("service=${entry.coordinate}|${entry.servicePath}|${entry.provider}")
                }
            }
        }
}
