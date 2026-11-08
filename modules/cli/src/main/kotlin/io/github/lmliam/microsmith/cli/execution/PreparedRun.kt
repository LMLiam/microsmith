package io.github.lmliam.microsmith.cli.execution

import io.github.lmliam.microsmith.cli.diagnostics.CliFailureCode
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunResult

internal sealed interface PreparedRun {
    data class Ready(val result: ScriptRunResult) : PreparedRun

    data class Failure(val code: CliFailureCode) : PreparedRun
}
