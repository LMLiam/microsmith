package io.github.lmliam.microsmith.runtime.scripting.host

import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunResult

internal data class ProcessIsolationExecutionOutcome(
    val exitCode: Int,
    val processOutput: String,
    val parsedResult: ScriptRunResult?,
)
