package me.liam.microsmith.runtime.scripting.host

import me.liam.microsmith.runtime.scripting.model.ScriptFailureType
import me.liam.microsmith.runtime.scripting.model.ScriptRunFailure
import me.liam.microsmith.runtime.scripting.model.ScriptRunResult

internal class ProcessIsolationFailureFactory {
    fun fromOutcome(outcome: ProcessIsolationExecutionOutcome): ScriptRunResult {
        if (outcome.exitCode == 0 && outcome.parsedResult != null) {
            return outcome.parsedResult
        }
        if (outcome.parsedResult is ScriptRunFailure) {
            return appendProcessOutput(outcome.parsedResult, outcome.processOutput)
        }
        return unknownFailure(outcome.exitCode, outcome.processOutput)
    }

    fun fromException(error: Exception): ScriptRunFailure {
        val message = error.message ?: error::class.simpleName ?: "unknown process isolation error"
        return ScriptRunFailure(
            diagnostics = listOf("Process isolation execution failed: $message"),
            type = ScriptFailureType.HOST,
        )
    }

    private fun appendProcessOutput(failure: ScriptRunFailure, processOutput: String): ScriptRunFailure {
        if (processOutput.isEmpty()) {
            return failure
        }
        return failure.copy(diagnostics = failure.diagnostics + "Process stderr/stdout: $processOutput")
    }

    private fun unknownFailure(exitCode: Int, processOutput: String): ScriptRunFailure {
        val diagnostics =
            buildList {
                add("Process isolation execution failed with exit code $exitCode.")
                if (processOutput.isNotEmpty()) {
                    add("Process stderr/stdout: $processOutput")
                }
            }
        return ScriptRunFailure(diagnostics = diagnostics, type = ScriptFailureType.HOST)
    }
}
