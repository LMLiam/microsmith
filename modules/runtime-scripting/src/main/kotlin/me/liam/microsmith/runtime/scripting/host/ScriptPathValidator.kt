package me.liam.microsmith.runtime.scripting.host

import me.liam.microsmith.runtime.scripting.model.ScriptFailureType
import me.liam.microsmith.runtime.scripting.model.ScriptRunFailure
import java.nio.file.Files
import java.nio.file.Path

internal object ScriptPathValidator {
    fun validate(scriptPath: Path): ScriptRunFailure? = when {
        !Files.exists(scriptPath) ->
            ScriptRunFailure(
                diagnostics = listOf("Script file '$scriptPath' does not exist."),
                type = ScriptFailureType.VALIDATION,
            )

        !Files.isRegularFile(scriptPath) ->
            ScriptRunFailure(
                diagnostics = listOf("Script path '$scriptPath' is not a file."),
                type = ScriptFailureType.VALIDATION,
            )

        else -> ScriptDirectivePolicy.validate(scriptPath)
    }
}
