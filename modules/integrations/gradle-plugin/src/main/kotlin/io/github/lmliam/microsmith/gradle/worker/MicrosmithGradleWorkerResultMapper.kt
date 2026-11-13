package io.github.lmliam.microsmith.gradle.worker

import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunFailure
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunResult
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunSuccess

internal fun ScriptRunResult.toWorkerResult(): MicrosmithGradleWorkerResult =
    when (this) {
        is ScriptRunSuccess ->
            MicrosmithGradleWorkerSuccess(
                warnings = warnings,
                cacheHit = cacheHit,
                elapsedMillis = elapsedMillis,
                generatedRoots = generatedRoots,
            )

        is ScriptRunFailure ->
            MicrosmithGradleWorkerFailure(
                diagnostics =
                    diagnostics.map { diagnostic ->
                        MicrosmithGradleWorkerDiagnostic(message = diagnostic.message, code = diagnostic.code)
                    },
                type = type.name,
            )
    }
