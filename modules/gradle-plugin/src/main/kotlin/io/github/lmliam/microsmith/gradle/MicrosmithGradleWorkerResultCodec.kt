package io.github.lmliam.microsmith.gradle

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

internal class MicrosmithGradleWorkerResultCodec {
    fun write(path: Path, result: MicrosmithGradleWorkerResult) {
        val properties = when (result) {
            is MicrosmithGradleWorkerSuccess -> successProperties(result)
            is MicrosmithGradleWorkerFailure -> failureProperties(result)
        }
        path.parent?.let(Files::createDirectories)
        Files.newOutputStream(path).use { output -> properties.store(output, null) }
    }

    fun read(path: Path): MicrosmithGradleWorkerResult {
        val properties = Properties()
        Files.newInputStream(path).use { input -> properties.load(input) }
        return when (properties.getProperty(RESULT_STATUS)?.trim()) {
            RESULT_STATUS_SUCCESS -> MicrosmithGradleWorkerSuccess(
                warnings = properties.readValues(RESULT_WARNING_COUNT, RESULT_WARNING_PREFIX),
                cacheHit = properties.requiredBoolean(RESULT_CACHE_HIT),
                elapsedMillis = properties.requiredLong(RESULT_ELAPSED_MILLIS),
                generatedRoots =
                properties
                    .readValues(RESULT_GENERATED_ROOT_COUNT, RESULT_GENERATED_ROOT_PREFIX)
                    .map(Path::of),
            )

            RESULT_STATUS_FAILURE -> MicrosmithGradleWorkerFailure(
                diagnostics = properties.readValues(RESULT_DIAGNOSTIC_COUNT, RESULT_DIAGNOSTIC_PREFIX),
                type = properties.getProperty(RESULT_FAILURE_TYPE) ?: missingProperty(RESULT_FAILURE_TYPE),
            )

            else -> error("Missing or invalid worker result status.")
        }
    }

    private fun successProperties(result: MicrosmithGradleWorkerSuccess): Properties = Properties().apply {
        this[RESULT_STATUS] = RESULT_STATUS_SUCCESS
        this[RESULT_ELAPSED_MILLIS] = result.elapsedMillis.toString()
        this[RESULT_CACHE_HIT] = result.cacheHit.toString()
        this[RESULT_WARNING_COUNT] = result.warnings.size.toString()
        this[RESULT_GENERATED_ROOT_COUNT] = result.generatedRoots.size.toString()
        result.warnings.forEachIndexed { index, warning ->
            this["$RESULT_WARNING_PREFIX$index"] = warning
        }
        result.generatedRoots.forEachIndexed { index, generatedRoot ->
            this["$RESULT_GENERATED_ROOT_PREFIX$index"] = generatedRoot.toString()
        }
    }

    private fun failureProperties(result: MicrosmithGradleWorkerFailure): Properties = Properties().apply {
        this[RESULT_STATUS] = RESULT_STATUS_FAILURE
        this[RESULT_FAILURE_TYPE] = result.type
        this[RESULT_DIAGNOSTIC_COUNT] = result.diagnostics.size.toString()
        result.diagnostics.forEachIndexed { index, diagnostic ->
            this["$RESULT_DIAGNOSTIC_PREFIX$index"] = diagnostic
        }
    }
}

private const val RESULT_STATUS = "result.status"
private const val RESULT_STATUS_SUCCESS = "success"
private const val RESULT_STATUS_FAILURE = "failure"
private const val RESULT_ELAPSED_MILLIS = "result.elapsedMillis"
private const val RESULT_CACHE_HIT = "result.cacheHit"
private const val RESULT_WARNING_COUNT = "result.warnings.count"
private const val RESULT_WARNING_PREFIX = "result.warning."
private const val RESULT_GENERATED_ROOT_COUNT = "result.generatedRoots.count"
private const val RESULT_GENERATED_ROOT_PREFIX = "result.generatedRoot."
private const val RESULT_FAILURE_TYPE = "result.failureType"
private const val RESULT_DIAGNOSTIC_COUNT = "result.diagnostics.count"
private const val RESULT_DIAGNOSTIC_PREFIX = "result.diagnostic."
