package io.github.lmliam.microsmith.gradle.worker

import java.util.Locale

internal data class MicrosmithGradleWorkerFailure(
    val diagnostics: List<MicrosmithGradleWorkerDiagnostic>,
    val type: String,
) : MicrosmithGradleWorkerResult {
    fun describe(): String =
        if (type == RESOLUTION_TYPE) {
            describeResolutionFailure()
        } else {
            describeGenerationFailure()
        }

    private fun describeResolutionFailure(): String = buildString {
        appendLine("Microsmith model resolution failed:")
        diagnostics.forEach { diagnostic ->
            appendLine(diagnostic.code?.let { code -> "[$code] ${diagnostic.message}" } ?: diagnostic.message)
        }
    }
        .trimEnd()

    private fun describeGenerationFailure(): String = buildString {
        appendLine("Microsmith generation failed (${type.lowercase(Locale.ROOT)}).")
        diagnostics.forEach { diagnostic -> appendLine(diagnostic.message) }
    }
        .trimEnd()
}

private const val RESOLUTION_TYPE = "RESOLUTION"
