package me.liam.microsmith.runtime.scripting.host

import me.liam.microsmith.runtime.scripting.model.ScriptRunFailure
import java.nio.file.Files
import java.nio.file.Path

internal object ScriptPathValidator {
    fun validate(scriptPath: Path): ScriptRunFailure? =
        when {
            !Files.exists(scriptPath) -> ScriptRunFailure(listOf("Script file '$scriptPath' does not exist."))
            !Files.isRegularFile(scriptPath) -> ScriptRunFailure(listOf("Script path '$scriptPath' is not a file."))
            else -> null
        }
}
