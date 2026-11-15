package io.github.lmliam.microsmith.runtime.scripting.host.isolation.process

import dev.zacsweers.metro.Inject
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import java.nio.file.FileSystem
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.createDirectories
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

@Inject
internal class ProcessIsolationRequestCodec(private val fileSystem: FileSystem) {
    fun write(path: Path, request: ProcessIsolationRequest) {
        val properties = Properties()
        properties[ProcessIsolationPropertyNames.REQUEST_SCRIPT] = request.scriptPath.toString()
        properties[ProcessIsolationPropertyNames.REQUEST_OUTPUT_DIR] = request.outputPath.toString()
        properties[ProcessIsolationPropertyNames.REQUEST_CACHE_DIR] = request.cacheDirectory.toString()

        val pluginClasspath = request.request.pluginClasspath
        properties[ProcessIsolationPropertyNames.REQUEST_PLUGIN_CLASSPATH_COUNT] = pluginClasspath.size.toString()
        pluginClasspath.forEachIndexed { index, pluginPath ->
            properties["${ProcessIsolationPropertyNames.REQUEST_PLUGIN_CLASSPATH_PREFIX}$index"] = pluginPath.toString()
        }

        val variables = request.request.variables.toList().sortedBy { it.first }
        properties[ProcessIsolationPropertyNames.REQUEST_VARIABLE_COUNT] = variables.size.toString()
        variables.forEachIndexed { index, (key, value) ->
            properties["${ProcessIsolationPropertyNames.REQUEST_VARIABLE_KEY_PREFIX}$index"] = key
            properties["${ProcessIsolationPropertyNames.REQUEST_VARIABLE_VALUE_PREFIX}$index"] = value
        }

        val flags = request.request.flags.toList().sorted()
        properties[ProcessIsolationPropertyNames.REQUEST_FLAG_COUNT] = flags.size.toString()
        flags.forEachIndexed { index, flag ->
            properties["${ProcessIsolationPropertyNames.REQUEST_FLAG_PREFIX}$index"] = flag
        }

        path.parent?.let(Path::createDirectories)
        path.outputStream().use { output -> properties.store(output, "Microsmith process isolation request") }
    }

    fun read(path: Path): ProcessIsolationRequest {
        val properties = Properties().also { loaded -> path.inputStream().use { input -> loaded.load(input) } }

        val scriptPath = properties.requiredPath(fileSystem, ProcessIsolationPropertyNames.REQUEST_SCRIPT)
        val outputPath = properties.requiredPath(fileSystem, ProcessIsolationPropertyNames.REQUEST_OUTPUT_DIR)
        val cacheDirectory = properties.requiredPath(fileSystem, ProcessIsolationPropertyNames.REQUEST_CACHE_DIR)
        val pluginClasspath =
            properties
                .readIndexedList(
                    countKey = ProcessIsolationPropertyNames.REQUEST_PLUGIN_CLASSPATH_COUNT,
                    keyPrefix = ProcessIsolationPropertyNames.REQUEST_PLUGIN_CLASSPATH_PREFIX,
                )
                .map(fileSystem::getPath)
        val variables =
            properties.readIndexedMap(
                countKey = ProcessIsolationPropertyNames.REQUEST_VARIABLE_COUNT,
                keyPrefix = ProcessIsolationPropertyNames.REQUEST_VARIABLE_KEY_PREFIX,
                valuePrefix = ProcessIsolationPropertyNames.REQUEST_VARIABLE_VALUE_PREFIX,
            )
        val flags =
            properties
                .readIndexedList(
                    countKey = ProcessIsolationPropertyNames.REQUEST_FLAG_COUNT,
                    keyPrefix = ProcessIsolationPropertyNames.REQUEST_FLAG_PREFIX,
                )
                .toSet()

        return ProcessIsolationRequest(
            request =
                ScriptRunRequest(
                    script = scriptPath,
                    outputDir = outputPath,
                    variables = variables,
                    flags = flags,
                    pluginClasspath = pluginClasspath,
                ),
            scriptPath = scriptPath,
            outputPath = outputPath,
            cacheDirectory = cacheDirectory,
        )
    }
}
