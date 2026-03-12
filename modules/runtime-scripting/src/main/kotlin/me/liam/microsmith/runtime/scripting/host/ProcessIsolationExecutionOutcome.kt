package me.liam.microsmith.runtime.scripting.host

import me.liam.microsmith.runtime.scripting.model.ScriptRunResult

internal data class ProcessIsolationExecutionOutcome(
    val exitCode: Int,
    val processOutput: String,
    val parsedResult: ScriptRunResult?,
)
