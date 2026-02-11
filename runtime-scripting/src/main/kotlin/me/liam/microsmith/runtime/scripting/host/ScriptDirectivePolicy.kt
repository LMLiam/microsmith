package me.liam.microsmith.runtime.scripting.host

import me.liam.microsmith.runtime.scripting.model.ScriptRunFailure
import java.nio.file.Files
import java.nio.file.Path

private val DIRECT_FILE_DIRECTIVE =
    Regex(
        pattern = """^\s*@file:\s*(?:kotlin\.script\.experimental\.dependencies\.)?(DependsOn|Repository)\b""",
    )
private val GROUP_FILE_DIRECTIVE_START = Regex(pattern = """^\s*@file:\s*\[""")
private val GROUP_FILE_DIRECTIVE_END = Regex(pattern = """]""")
private val GROUP_FORBIDDEN_DIRECTIVE =
    Regex(
        pattern = """(?:kotlin\.script\.experimental\.dependencies\.)?(DependsOn|Repository)\b""",
    )

internal object ScriptDirectivePolicy {
    fun validate(scriptPath: Path): ScriptRunFailure? {
        val violations = collectViolations(Files.readAllLines(scriptPath))

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

    private fun collectViolations(lines: List<String>): List<String> {
        val violations = mutableListOf<String>()

        var groupStartLine: Int? = null
        val groupBuffer = mutableListOf<String>()

        lines.forEachIndexed { index, line ->
            val directMatch = DIRECT_FILE_DIRECTIVE.find(line)
            if (directMatch != null) {
                val directive = directMatch.groupValues[1]
                violations += violationMessage(index + 1, directive)
            }

            when {
                groupStartLine == null && GROUP_FILE_DIRECTIVE_START.containsMatchIn(line) -> {
                    groupStartLine = index + 1
                    groupBuffer += line
                    if (GROUP_FILE_DIRECTIVE_END.containsMatchIn(line)) {
                        val start = requireNotNull(groupStartLine)
                        violations += collectGroupedViolations(groupBuffer.joinToString("\n"), start)
                        groupStartLine = null
                        groupBuffer.clear()
                    }
                }

                groupStartLine != null -> {
                    groupBuffer += line
                    if (GROUP_FILE_DIRECTIVE_END.containsMatchIn(line)) {
                        val start = requireNotNull(groupStartLine)
                        violations += collectGroupedViolations(groupBuffer.joinToString("\n"), start)
                        groupStartLine = null
                        groupBuffer.clear()
                    }
                }
            }
        }

        return violations
    }

    private fun collectGroupedViolations(groupText: String, startLine: Int): List<String> {
        val matches = GROUP_FORBIDDEN_DIRECTIVE.findAll(groupText)
        return matches
            .map { match ->
                val newlineCountBeforeMatch = groupText.substring(0, match.range.first).count { it == '\n' }
                val lineNumber = startLine + newlineCountBeforeMatch
                val directive = match.groupValues[1]
                violationMessage(lineNumber, directive)
            }.distinct()
            .toList()
    }

    private fun violationMessage(lineNumber: Int, directive: String): String =
        "Line $lineNumber: @file:$directive is disallowed. Use --plugin/--plugin-jar instead."
}
