package me.liam.microsmith.gradle

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

internal class MicrosmithGradleWorkerRequestCodec {
    fun write(path: Path, request: MicrosmithGradleWorkerRequest) {
        val properties = Properties()
        properties[SCRIPT_PATH] = request.scriptPath.toString()
        properties[OUTPUT_PATH] = request.outputPath.toString()
        properties[CACHE_DIRECTORY] = request.cacheDirectory.toString()
        properties[PLUGIN_CLASSPATH_COUNT] = request.pluginClasspath.size.toString()
        request.pluginClasspath.forEachIndexed { index, pluginPath ->
            properties["$PLUGIN_CLASSPATH_PREFIX$index"] = pluginPath.toString()
        }
        properties[VARIABLE_COUNT] = request.variables.size.toString()
        request.variables.entries.sortedBy { (key, _) -> key }.forEachIndexed { index, (key, value) ->
            properties["$VARIABLE_KEY_PREFIX$index"] = key
            properties["$VARIABLE_VALUE_PREFIX$index"] = value
        }
        properties[FLAG_COUNT] = request.flags.size.toString()
        request.flags.sorted().forEachIndexed { index, flag ->
            properties["$FLAG_PREFIX$index"] = flag
        }
        path.parent?.let(Files::createDirectories)
        Files.newOutputStream(path).use { output -> properties.store(output, null) }
    }

    fun read(path: Path): MicrosmithGradleWorkerRequest {
        val properties = Properties()
        Files.newInputStream(path).use { input -> properties.load(input) }
        return MicrosmithGradleWorkerRequest(
            scriptPath = properties.requiredPath(SCRIPT_PATH),
            outputPath = properties.requiredPath(OUTPUT_PATH),
            cacheDirectory = properties.requiredPath(CACHE_DIRECTORY),
            variables =
            properties.readPairs(
                countKey = VARIABLE_COUNT,
                keyPrefix = VARIABLE_KEY_PREFIX,
                valuePrefix = VARIABLE_VALUE_PREFIX,
            ),
            flags =
            properties.readValues(
                countKey = FLAG_COUNT,
                valuePrefix = FLAG_PREFIX,
            ).toSortedSet(),
            pluginClasspath =
            properties.readValues(
                countKey = PLUGIN_CLASSPATH_COUNT,
                valuePrefix = PLUGIN_CLASSPATH_PREFIX,
            ).map(Path::of),
        )
    }
}

private const val SCRIPT_PATH = "request.scriptPath"
private const val OUTPUT_PATH = "request.outputPath"
private const val CACHE_DIRECTORY = "request.cacheDirectory"
private const val PLUGIN_CLASSPATH_COUNT = "request.pluginClasspath.count"
private const val PLUGIN_CLASSPATH_PREFIX = "request.pluginClasspath."
private const val VARIABLE_COUNT = "request.variables.count"
private const val VARIABLE_KEY_PREFIX = "request.variables.key."
private const val VARIABLE_VALUE_PREFIX = "request.variables.value."
private const val FLAG_COUNT = "request.flags.count"
private const val FLAG_PREFIX = "request.flags."
