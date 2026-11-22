package io.github.lmliam.microsmith.runtime.scripting.host.paths

import dev.zacsweers.metro.Inject
import io.github.lmliam.microsmith.runtime.scripting.host.configuration.ScriptDirectivePolicy
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptFailureType
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunFailure
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

@Inject
internal class ScriptPathValidator(private val directivePolicy: ScriptDirectivePolicy) {
    fun validate(scriptPath: Path): ScriptRunFailure? =
        when {
            !scriptPath.exists() ->
                ScriptRunFailure.withMessages(
                    messages = listOf("Script file '$scriptPath' does not exist."),
                    type = ScriptFailureType.VALIDATION,
                )

            !scriptPath.isRegularFile() ->
                ScriptRunFailure.withMessages(
                    messages = listOf("Script path '$scriptPath' is not a file."),
                    type = ScriptFailureType.VALIDATION,
                )

            else -> directivePolicy.validate(scriptPath)
        }
}
