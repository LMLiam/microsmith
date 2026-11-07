package io.github.lmliam.microsmith.build.cli

import java.io.File
import java.util.jar.JarFile
import java.util.zip.GZIPInputStream
import java.util.zip.ZipFile
import org.apache.tools.tar.TarInputStream
import org.gradle.api.GradleException

internal data class BundledServiceEntry(
    val coordinate: String,
    val servicePath: String,
    val provider: String,
) : Comparable<BundledServiceEntry> {
    override fun compareTo(other: BundledServiceEntry): Int = COMPARATOR.compare(this, other)

    companion object {
        private val COMPARATOR =
            compareBy<BundledServiceEntry>(
                { it.coordinate },
                { it.servicePath },
                { it.provider },
            )
    }
}

internal object CliBundledPluginProfile {
    fun plugins(
        coordinates: List<String>,
        jars: List<File>,
    ): List<Pair<String, File>> {
        if (coordinates.size != jars.size) {
            throw GradleException(
                "Bundled plugin coordinates (${coordinates.size}) do not match the bundled plugin jars (${jars.size}).",
            )
        }
        return coordinates.zip(jars)
    }

    fun serviceEntries(plugins: List<Pair<String, File>>): List<BundledServiceEntry> =
        plugins.flatMap { (coordinate, jarFile) ->
            CliBundledPluginCatalogVerifier.collectServiceEntries(jarFile, coordinate)
        }

    fun catalogText(
        cliVersion: String,
        formatVersion: Int,
        plugins: List<Pair<String, File>>,
    ): String =
        buildString {
            appendLine("# Microsmith bundled plugin profile")
            appendLine("# Plugin coordinates are pinned to the CLI release version for deterministic runtime behavior.")
            appendLine("format=$formatVersion")
            appendLine("cliVersion=$cliVersion")

            val serviceEntriesByCoordinate = serviceEntries(plugins).groupBy { it.coordinate }

            for ((coordinate, _) in plugins) {
                appendLine("plugin=$coordinate")
                val entries = serviceEntriesByCoordinate[coordinate].orEmpty()
                for (entry in entries) {
                    appendLine("service=${entry.coordinate}|${entry.servicePath}|${entry.provider}")
                }
            }
        }
}

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

        catalogText.lineSequence().withIndex().forEach { (index, rawLine) ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            val lineNumber = index + 1

            val (key, value) =
                line.split('=', limit = 2).takeIf { it.size == 2 }
                    ?: throw GradleException("Unrecognized entry at $source:$lineNumber: '$line'")

            when (key) {
                "format" -> formatValue = value
                "cliVersion" -> cliVersion = value
                "plugin" -> pluginCoordinates += value
                "service" -> serviceEntries += parseServiceDeclaration(value, source, lineNumber)
                else -> throw GradleException("Unrecognized entry at $source:$lineNumber: '$line'")
            }
        }

        if (formatValue != expectedFormatVersion.toString()) {
            throw GradleException(
                "Bundled plugin catalog '$source' has format '$formatValue', expected '$expectedFormatVersion'.",
            )
        }
        if (cliVersion != expectedCliVersion) {
            throw GradleException(
                "Bundled plugin catalog '$source' has cliVersion '$cliVersion', expected '$expectedCliVersion'.",
            )
        }
        if (pluginCoordinates != expectedCoordinates) {
            throw GradleException(
                "Bundled plugin catalog '$source' defines coordinates $pluginCoordinates, expected $expectedCoordinates.",
            )
        }
        if (serviceEntries.sorted() != expectedServiceEntries.sorted()) {
            throw GradleException(
                "Bundled plugin catalog '$source' service declarations do not match expected providers.",
            )
        }
    }

    fun collectServiceEntries(
        jarFile: File,
        coordinate: String,
    ): List<BundledServiceEntry> = jarFile.readServiceEntries(coordinate).sorted()

    fun collectZipEntries(zipArchive: File): Set<String> = zipArchive.readZipEntries()

    fun collectTarGzEntries(tarGzArchive: File): Set<String> = tarGzArchive.readTarGzEntries()

    private fun parseServiceDeclaration(
        value: String,
        source: String,
        lineNumber: Int,
    ): BundledServiceEntry {
        val parts = value.split('|')
        if (parts.size != 3 || parts.any { it.isBlank() }) {
            throw GradleException(
                "Invalid service declaration at $source:$lineNumber. Expected 'service=<coordinate>|<service-path>|<provider>'.",
            )
        }
        return BundledServiceEntry(coordinate = parts[0], servicePath = parts[1], provider = parts[2])
    }
}

private fun File.readServiceEntries(coordinate: String): List<BundledServiceEntry> =
    JarFile(this).use { jar ->
        jar
            .entries()
            .asSequence()
            .filter { !it.isDirectory && it.name.startsWith("META-INF/services/") }
            .flatMap { entry ->
                jar.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { reader ->
                    reader
                        .lineSequence()
                        .map { it.substringBefore('#').trim() }
                        .filter { it.isNotEmpty() }
                        .map { provider ->
                            BundledServiceEntry(
                                coordinate = coordinate,
                                servicePath = entry.name,
                                provider = provider,
                            )
                        }.toList()
                }
            }.toList()
    }

private fun File.readZipEntries(): Set<String> =
    ZipFile(this).use { zip ->
        zip
            .entries()
            .asSequence()
            .filterNot { it.isDirectory }
            .mapTo(linkedSetOf()) { it.name }
    }

private fun File.readTarGzEntries(): Set<String> =
    TarInputStream(GZIPInputStream(inputStream().buffered())).use { tar ->
        generateSequence { tar.nextEntry }
            .filterNot { it.isDirectory }
            .mapTo(linkedSetOf()) { it.name }
    }
