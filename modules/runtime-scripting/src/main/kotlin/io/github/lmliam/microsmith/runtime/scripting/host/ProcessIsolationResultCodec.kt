package io.github.lmliam.microsmith.runtime.scripting.host

import io.github.lmliam.microsmith.runtime.scripting.model.ScriptFailureType
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunFailure
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunResult
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunSuccess
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

internal class ProcessIsolationResultCodec {
    fun write(path: Path, result: ScriptRunResult) {
        val properties = Properties()
        when (result) {
            is ScriptRunSuccess -> writeSuccess(properties, result)
            is ScriptRunFailure -> writeFailure(properties, result)
        }

        path.parent?.let(Files::createDirectories)
        Files.newOutputStream(path).use { output ->
            properties.store(output, "Microsmith process isolation result")
        }
    }

    fun read(path: Path): ScriptRunResult {
        val properties = Properties().also { loaded ->
            Files.newInputStream(path).use { input ->
                loaded.load(input)
            }
        }

        return when (properties.getProperty(ProcessIsolationPropertyNames.RESULT_STATUS)?.trim()) {
            ProcessIsolationPropertyNames.RESULT_STATUS_SUCCESS -> readSuccess(properties)
            ProcessIsolationPropertyNames.RESULT_STATUS_FAILURE -> readFailure(properties)
            else -> error(
                "Missing or invalid '${ProcessIsolationPropertyNames.RESULT_STATUS}' in process isolation result.",
            )
        }
    }

    private fun writeSuccess(properties: Properties, result: ScriptRunSuccess) {
        properties[ProcessIsolationPropertyNames.RESULT_STATUS] = ProcessIsolationPropertyNames.RESULT_STATUS_SUCCESS
        properties[ProcessIsolationPropertyNames.RESULT_ELAPSED_MILLIS] = result.elapsedMillis.toString()
        properties[ProcessIsolationPropertyNames.RESULT_CACHE_HIT] = result.cacheHit.toString()
        properties[ProcessIsolationPropertyNames.RESULT_WARNING_COUNT] = result.warnings.size.toString()
        result.warnings.forEachIndexed { index, warning ->
            properties["${ProcessIsolationPropertyNames.RESULT_WARNING_PREFIX}$index"] = warning
        }
    }

    private fun writeFailure(properties: Properties, result: ScriptRunFailure) {
        properties[ProcessIsolationPropertyNames.RESULT_STATUS] = ProcessIsolationPropertyNames.RESULT_STATUS_FAILURE
        properties[ProcessIsolationPropertyNames.RESULT_DIAGNOSTIC_COUNT] = result.diagnostics.size.toString()
        properties[ProcessIsolationPropertyNames.RESULT_FAILURE_TYPE] = result.type.name
        result.diagnostics.forEachIndexed { index, diagnostic ->
            properties["${ProcessIsolationPropertyNames.RESULT_DIAGNOSTIC_PREFIX}$index"] = diagnostic
        }
    }

    private fun readSuccess(properties: Properties): ScriptRunSuccess = ScriptRunSuccess(
        warnings =
        properties.readIndexedList(
            countKey = ProcessIsolationPropertyNames.RESULT_WARNING_COUNT,
            keyPrefix = ProcessIsolationPropertyNames.RESULT_WARNING_PREFIX,
        ),
        cacheHit = properties.requiredBoolean(ProcessIsolationPropertyNames.RESULT_CACHE_HIT),
        elapsedMillis = properties.requiredLong(ProcessIsolationPropertyNames.RESULT_ELAPSED_MILLIS),
    )

    private fun readFailure(properties: Properties): ScriptRunFailure = ScriptRunFailure(
        diagnostics =
        properties.readIndexedList(
            countKey = ProcessIsolationPropertyNames.RESULT_DIAGNOSTIC_COUNT,
            keyPrefix = ProcessIsolationPropertyNames.RESULT_DIAGNOSTIC_PREFIX,
        ),
        type = parseFailureType(properties.getProperty(ProcessIsolationPropertyNames.RESULT_FAILURE_TYPE)),
    )

    private fun parseFailureType(raw: String?): ScriptFailureType = runCatching {
        raw?.trim()?.takeIf(String::isNotEmpty)?.let(ScriptFailureType::valueOf)
    }.getOrNull() ?: ScriptFailureType.HOST
}
