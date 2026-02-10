package me.liam.microsmith.cli.parsing

import java.nio.file.Path

private const val PLUGIN_COORDINATE_PART_COUNT = 3

internal fun validateOutputValue(
    value: String?,
    outputDirAlreadySet: Boolean
): String? =
    when {
        value == null || value.startsWith("--") -> "Missing value for --out option."
        outputDirAlreadySet -> "--out option may only be specified once."
        else -> null
    }

internal fun parseVariableValue(value: String?): ParsedVariable =
    when {
        value == null || value.startsWith("--") -> ParsedVariable(error = "Missing value for --var option.")

        else -> {
            val separatorIndex = value.indexOf('=')
            val key =
                if (separatorIndex > 0) {
                    value.take(separatorIndex).trim()
                } else {
                    ""
                }
            if (separatorIndex <= 0 || key.isBlank()) {
                ParsedVariable(error = "Invalid --var value '$value'. Expected key=value.")
            } else {
                ParsedVariable(
                    key = key,
                    value = value.substring(separatorIndex + 1)
                )
            }
        }
    }

internal fun parseFlagValue(value: String?): String? =
    value
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

internal data class ParsedToken(
    val nextIndex: Int,
    val error: String? = null
)

internal class RunOptionsState {
    var outputDir: Path? = null
    val variables = linkedMapOf<String, String>()
    val flags = linkedSetOf<String>()
    val plugins = linkedSetOf<String>()
    val pluginJars = linkedSetOf<Path>()
    var offline: Boolean = false
    var repositoryOverride: String? = null
    var error: String? = null

    fun toParsedRunOptions(): ParsedRunOptions =
        ParsedRunOptions(
            outputDir = outputDir,
            variables = variables.toMap(),
            flags = flags.toSet(),
            plugins = plugins.toSet(),
            pluginJars = pluginJars.toSet(),
            offline = offline,
            repositoryOverride = repositoryOverride,
            error = error
        )
}
