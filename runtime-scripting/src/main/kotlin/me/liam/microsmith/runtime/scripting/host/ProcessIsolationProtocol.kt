package me.liam.microsmith.runtime.scripting.host

import me.liam.microsmith.runtime.scripting.model.ScriptFailureType
import me.liam.microsmith.runtime.scripting.model.ScriptRunFailure
import me.liam.microsmith.runtime.scripting.model.ScriptRunRequest
import me.liam.microsmith.runtime.scripting.model.ScriptRunResult
import me.liam.microsmith.runtime.scripting.model.ScriptRunSuccess
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

private const val REQUEST_SCRIPT = "request.script"
private const val REQUEST_OUTPUT_DIR = "request.outputDir"
private const val REQUEST_CACHE_DIR = "request.cacheDir"
private const val REQUEST_PLUGIN_CLASSPATH_COUNT = "request.pluginClasspath.count"
private const val REQUEST_PLUGIN_CLASSPATH_PREFIX = "request.pluginClasspath."
private const val REQUEST_VARIABLE_COUNT = "request.variables.count"
private const val REQUEST_VARIABLE_KEY_PREFIX = "request.variables.key."
private const val REQUEST_VARIABLE_VALUE_PREFIX = "request.variables.value."
private const val REQUEST_FLAG_COUNT = "request.flags.count"
private const val REQUEST_FLAG_PREFIX = "request.flags."

private const val RESULT_STATUS = "result.status"
private const val RESULT_STATUS_SUCCESS = "success"
private const val RESULT_STATUS_FAILURE = "failure"
private const val RESULT_ELAPSED_MILLIS = "result.elapsedMillis"
private const val RESULT_CACHE_HIT = "result.cacheHit"
private const val RESULT_WARNING_COUNT = "result.warnings.count"
private const val RESULT_WARNING_PREFIX = "result.warnings."
private const val RESULT_DIAGNOSTIC_COUNT = "result.diagnostics.count"
private const val RESULT_DIAGNOSTIC_PREFIX = "result.diagnostics."
private const val RESULT_FAILURE_TYPE = "result.failure.type"

internal data class ProcessIsolationRequest(
    val request: ScriptRunRequest,
    val scriptPath: Path,
    val outputPath: Path,
    val cacheDirectory: Path,
)

internal object ProcessIsolationProtocol {
    fun writeRequest(path: Path, request: ProcessIsolationRequest) {
        val properties = Properties()
        properties[REQUEST_SCRIPT] = request.scriptPath.toString()
        properties[REQUEST_OUTPUT_DIR] = request.outputPath.toString()
        properties[REQUEST_CACHE_DIR] = request.cacheDirectory.toString()

        val pluginClasspath = request.request.pluginClasspath
        properties[REQUEST_PLUGIN_CLASSPATH_COUNT] = pluginClasspath.size.toString()
        pluginClasspath.forEachIndexed { index, pluginPath ->
            properties["$REQUEST_PLUGIN_CLASSPATH_PREFIX$index"] = pluginPath.toString()
        }

        val variables = request.request.variables.toList().sortedBy { it.first }
        properties[REQUEST_VARIABLE_COUNT] = variables.size.toString()
        variables.forEachIndexed { index, (key, value) ->
            properties["$REQUEST_VARIABLE_KEY_PREFIX$index"] = key
            properties["$REQUEST_VARIABLE_VALUE_PREFIX$index"] = value
        }

        val flags = request.request.flags.toList().sorted()
        properties[REQUEST_FLAG_COUNT] = flags.size.toString()
        flags.forEachIndexed { index, flag ->
            properties["$REQUEST_FLAG_PREFIX$index"] = flag
        }

        path.parent?.let(Files::createDirectories)
        Files.newOutputStream(path).use { output ->
            properties.store(output, "Microsmith process isolation request")
        }
    }

    fun readRequest(path: Path): ProcessIsolationRequest {
        val properties =
            Properties().also { loaded ->
                Files.newInputStream(path).use { input ->
                    loaded.load(input)
                }
            }

        val scriptPath = properties.requiredPath(REQUEST_SCRIPT)
        val outputPath = properties.requiredPath(REQUEST_OUTPUT_DIR)
        val cacheDirectory = properties.requiredPath(REQUEST_CACHE_DIR)
        val pluginClasspath =
            readIndexedList(
                properties = properties,
                countKey = REQUEST_PLUGIN_CLASSPATH_COUNT,
                keyPrefix = REQUEST_PLUGIN_CLASSPATH_PREFIX,
            ).map(Path::of)
        val variables =
            readIndexedMap(
                properties = properties,
                countKey = REQUEST_VARIABLE_COUNT,
                keyPrefix = REQUEST_VARIABLE_KEY_PREFIX,
                valuePrefix = REQUEST_VARIABLE_VALUE_PREFIX,
            )
        val flags =
            readIndexedList(
                properties = properties,
                countKey = REQUEST_FLAG_COUNT,
                keyPrefix = REQUEST_FLAG_PREFIX,
            ).toSet()

        val request =
            ScriptRunRequest(
                script = scriptPath,
                outputDir = outputPath,
                variables = variables,
                flags = flags,
                pluginClasspath = pluginClasspath,
            )

        return ProcessIsolationRequest(
            request = request,
            scriptPath = scriptPath,
            outputPath = outputPath,
            cacheDirectory = cacheDirectory,
        )
    }

    fun writeResult(path: Path, result: ScriptRunResult) {
        val properties = Properties()
        when (result) {
            is ScriptRunSuccess -> {
                properties[RESULT_STATUS] = RESULT_STATUS_SUCCESS
                properties[RESULT_ELAPSED_MILLIS] = result.elapsedMillis.toString()
                properties[RESULT_CACHE_HIT] = result.cacheHit.toString()
                properties[RESULT_WARNING_COUNT] = result.warnings.size.toString()
                result.warnings.forEachIndexed { index, warning ->
                    properties["$RESULT_WARNING_PREFIX$index"] = warning
                }
            }
            is ScriptRunFailure -> {
                properties[RESULT_STATUS] = RESULT_STATUS_FAILURE
                properties[RESULT_DIAGNOSTIC_COUNT] = result.diagnostics.size.toString()
                properties[RESULT_FAILURE_TYPE] = result.type.name
                result.diagnostics.forEachIndexed { index, diagnostic ->
                    properties["$RESULT_DIAGNOSTIC_PREFIX$index"] = diagnostic
                }
            }
        }

        path.parent?.let(Files::createDirectories)
        Files.newOutputStream(path).use { output ->
            properties.store(output, "Microsmith process isolation result")
        }
    }

    fun readResult(path: Path): ScriptRunResult {
        val properties =
            Properties().also { loaded ->
                Files.newInputStream(path).use { input ->
                    loaded.load(input)
                }
            }

        val status = properties.getProperty(RESULT_STATUS)?.trim()
        return when (status) {
            RESULT_STATUS_SUCCESS -> {
                val elapsedMillis =
                    properties.getProperty(RESULT_ELAPSED_MILLIS)?.trim()?.toLongOrNull()
                        ?: error("Missing or invalid '$RESULT_ELAPSED_MILLIS' in process isolation result.")
                val cacheHit =
                    properties.getProperty(RESULT_CACHE_HIT)?.trim()?.toBooleanStrictOrNull()
                        ?: error("Missing or invalid '$RESULT_CACHE_HIT' in process isolation result.")
                val warnings =
                    readIndexedList(
                        properties = properties,
                        countKey = RESULT_WARNING_COUNT,
                        keyPrefix = RESULT_WARNING_PREFIX,
                    )
                ScriptRunSuccess(warnings = warnings, cacheHit = cacheHit, elapsedMillis = elapsedMillis)
            }
            RESULT_STATUS_FAILURE -> {
                val diagnostics =
                    readIndexedList(
                        properties = properties,
                        countKey = RESULT_DIAGNOSTIC_COUNT,
                        keyPrefix = RESULT_DIAGNOSTIC_PREFIX,
                    )
                val failureType = parseFailureType(properties.getProperty(RESULT_FAILURE_TYPE))
                ScriptRunFailure(
                    diagnostics = diagnostics,
                    type = failureType,
                )
            }
            else -> error("Missing or invalid '$RESULT_STATUS' in process isolation result.")
        }
    }
}

private fun parseFailureType(raw: String?): ScriptFailureType = runCatching {
    raw?.trim()?.takeIf { it.isNotEmpty() }?.let(ScriptFailureType::valueOf)
}.getOrNull() ?: ScriptFailureType.HOST

private fun readIndexedList(properties: Properties, countKey: String, keyPrefix: String): List<String> {
    val count =
        properties.getProperty(countKey)?.trim()?.toIntOrNull()
            ?: error("Missing or invalid '$countKey'.")
    return (0 until count).map { index ->
        properties.getProperty("$keyPrefix$index")
            ?: error("Missing indexed entry '$keyPrefix$index'.")
    }
}

private fun readIndexedMap(
    properties: Properties,
    countKey: String,
    keyPrefix: String,
    valuePrefix: String,
): Map<String, String> {
    val count =
        properties.getProperty(countKey)?.trim()?.toIntOrNull()
            ?: error("Missing or invalid '$countKey'.")
    return (0 until count).associate { index ->
        val key = properties.getProperty("$keyPrefix$index") ?: error("Missing indexed entry '$keyPrefix$index'.")
        val value =
            properties.getProperty("$valuePrefix$index")
                ?: error("Missing indexed entry '$valuePrefix$index'.")
        key to value
    }
}

private fun Properties.requiredPath(key: String): Path =
    getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }?.let(Path::of)
        ?: error("Missing or invalid '$key'.")
