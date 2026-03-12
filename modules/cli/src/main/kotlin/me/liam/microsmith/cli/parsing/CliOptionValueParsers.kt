package me.liam.microsmith.cli.parsing

import me.liam.microsmith.cli.diagnostics.DiagnosticFormat
import me.liam.microsmith.runtime.scripting.model.ScriptIsolationMode
import java.nio.file.Path

private const val PLUGIN_COORDINATE_PART_COUNT = 3

internal fun parseVariableValue(value: String?): ParsedVariable = when {
    value == null || value.startsWith("--") -> ParsedVariable(error = "Missing value for --var option.")
    else -> parseVariableAssignment(value)
}

internal fun parseFlagValue(value: String?): String? = value
    ?.takeUnless { it.startsWith("--") }
    ?.trim()
    ?.takeIf { it.isNotBlank() }

internal fun parsePluginCoordinate(value: String?): String? {
    val coordinate =
        value
            ?.takeUnless { it.startsWith("--") }
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null

    val parts = coordinate.split(':')
    val isValid =
        parts.size == PLUGIN_COORDINATE_PART_COUNT &&
            parts.none { it.isBlank() } &&
            parts.none { it.any(Char::isWhitespace) }

    return coordinate.takeIf { isValid }
}

internal fun parseIsolationMode(value: String?): ScriptIsolationMode? {
    val normalized =
        value
            ?.takeUnless { it.startsWith("--") }
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null

    return ScriptIsolationMode.fromCliValue(normalized)
}

internal fun parseDiagnosticFormat(value: String?): DiagnosticFormat? {
    val normalized =
        value
            ?.takeUnless { it.startsWith("--") }
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.lowercase()
            ?: return null
    return DiagnosticFormat.parse(normalized)
}

internal inline fun parseSingleOccurrenceFlag(
    index: Int,
    alreadySpecified: Boolean,
    optionName: String,
    onSuccess: () -> Unit,
): ParsedToken {
    if (alreadySpecified) {
        return ParsedToken(nextIndex = index, error = "$optionName may only be specified once.")
    }

    onSuccess()
    return ParsedToken(nextIndex = index + 1)
}

internal inline fun parseRepoRootOption(
    args: List<String>,
    index: Int,
    alreadySpecified: Boolean,
    onSuccess: (Path) -> Unit,
): ParsedToken {
    val value = args.getOrNull(index + 1)
    if (value == null || value.startsWith("--")) {
        return ParsedToken(nextIndex = index, error = "Missing value for --repo-root option.")
    }
    if (alreadySpecified) {
        return ParsedToken(nextIndex = index, error = "--repo-root may only be specified once.")
    }

    onSuccess(Path.of(value))
    return ParsedToken(nextIndex = index + 2)
}

private fun parseVariableAssignment(value: String): ParsedVariable {
    val separatorIndex = value.indexOf('=')
    if (separatorIndex <= 0) {
        return ParsedVariable(error = "Invalid --var value '$value'. Expected key=value.")
    }

    val key = value.take(separatorIndex).trim()
    if (key.isBlank()) {
        return ParsedVariable(error = "Invalid --var value '$value'. Expected key=value.")
    }

    return ParsedVariable(
        key = key,
        value = value.substring(separatorIndex + 1),
    )
}
