package me.liam.microsmith.runtime.scripting.host

import me.liam.microsmith.runtime.scripting.model.ScriptRunFailure
import java.nio.file.Files
import java.nio.file.Path

private val FORBIDDEN_SCRIPT_DIRECTIVE =
    Regex(
        pattern = """^\s*@file:\s*(?:kotlin\.script\.experimental\.dependencies\.)?(DependsOn|Repository)\b""",
    )

internal object ScriptDirectivePolicy {
    fun validate(scriptPath: Path): ScriptRunFailure? {
        val violations =
            Files.readAllLines(scriptPath).mapIndexedNotNull { index, line ->
                val match = FORBIDDEN_SCRIPT_DIRECTIVE.find(line) ?: return@mapIndexedNotNull null
                val directive = match.groupValues[1]
                "Line ${index + 1}: @file:$directive is disallowed. Use --plugin/--plugin-jar instead."
            }

        return if (violations.isEmpty()) {
            null
        } else {
            ScriptRunFailure(
                listOf(
                    "Script dependency directives are blocked by default for security hardening.",
                ) + violations,
            )
        }
    }
}
