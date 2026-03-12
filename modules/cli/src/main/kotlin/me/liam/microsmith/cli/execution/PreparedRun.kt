package me.liam.microsmith.cli.execution

import me.liam.microsmith.cli.diagnostics.CliFailureCode
import me.liam.microsmith.runtime.scripting.model.ScriptRunResult

internal sealed interface PreparedRun {
    data class Ready(val result: ScriptRunResult) : PreparedRun

    data class Failure(val code: CliFailureCode) : PreparedRun
}
