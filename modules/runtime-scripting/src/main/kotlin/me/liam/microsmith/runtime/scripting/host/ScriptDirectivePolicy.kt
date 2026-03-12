package me.liam.microsmith.runtime.scripting.host

import me.liam.microsmith.runtime.scripting.model.ScriptFailureType
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
        if (violations.isEmpty()) {
            return null
        }
        return ScriptRunFailure(
            diagnostics =
            listOf(
                "Script dependency directives are blocked by default for security hardening.",
            ) + violations,
            type = ScriptFailureType.VALIDATION,
        )
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

            if (groupStartLine == null && GROUP_FILE_DIRECTIVE_START.containsMatchIn(line)) {
                groupStartLine = index + 1
                groupBuffer += line
                flushGroupedViolationsIfComplete(groupStartLine, groupBuffer)?.let { groupViolations ->
                    violations += groupViolations
                    groupStartLine = null
                }
                return@forEachIndexed
            }

            if (groupStartLine == null) {
                return@forEachIndexed
            }

            groupBuffer += line
            flushGroupedViolationsIfComplete(groupStartLine, groupBuffer)?.let { groupViolations ->
                violations += groupViolations
                groupStartLine = null
            }
        }

        return violations
    }

    private fun flushGroupedViolationsIfComplete(
        groupStartLine: Int?,
        groupBuffer: MutableList<String>,
    ): List<String>? {
        if (!GROUP_FILE_DIRECTIVE_END.containsMatchIn(groupBuffer.last())) {
            return null
        }

        val start = requireNotNull(groupStartLine)
        val groupViolations = collectGroupedViolations(groupBuffer.joinToString("\n"), start)
        groupBuffer.clear()
        return groupViolations
    }

    private fun collectGroupedViolations(groupText: String, startLine: Int): List<String> {
        val matches = GROUP_FORBIDDEN_DIRECTIVE.findAll(groupText)
        return matches
            .map { match ->
                val newlineCountBeforeMatch = groupText.take(match.range.first).count { it == '\n' }
                val lineNumber = startLine + newlineCountBeforeMatch
                val directive = match.groupValues[1]
                violationMessage(lineNumber, directive)
            }.distinct()
            .toList()
    }

    private fun violationMessage(lineNumber: Int, directive: String): String =
        "Line $lineNumber: @file:$directive is disallowed. Use --plugin/--plugin-jar instead."
}
