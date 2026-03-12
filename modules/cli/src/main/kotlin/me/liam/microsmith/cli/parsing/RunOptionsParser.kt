package me.liam.microsmith.cli.parsing

import java.nio.file.Path

internal fun parseRunOptions(args: List<String>, startIndex: Int): ParsedRunOptions {
    val state = RunOptionsState()
    var index = startIndex
    while (index < args.size && state.error == null) {
        val parsedToken = parseRunOptionToken(args = args, index = index, state = state)
        index = parsedToken.nextIndex
        if (parsedToken.error != null) {
            state.error = parsedToken.error
        }
    }
    return state.toParsedRunOptions()
}

private fun parseRunOptionToken(args: List<String>, index: Int, state: RunOptionsState): ParsedToken =
    when (val token = args[index]) {
        OUTPUT_OPTION -> parseOutputOption(args, index, state)
        VARIABLE_OPTION -> parseVariableOption(args, index, state)
        FLAG_OPTION -> parseFlagOption(args, index, state)
        PLUGIN_OPTION -> parsePluginOption(args, index, state)
        PLUGIN_JAR_OPTION -> parsePluginJarOption(args, index, state)
        OFFLINE_OPTION -> parseOfflineOption(index, state)
        REPOSITORY_OPTION -> parseRepositoryOption(args, index, state)
        ISOLATION_OPTION -> parseIsolationOption(args, index, state)
        DIAGNOSTICS_OPTION -> parseDiagnosticsOption(args, index, state)
        VERBOSE_OPTION -> parseVerboseOption(index, state)
        EVENT_LOG_OPTION -> parseEventLogOption(args, index, state)
        else -> ParsedToken(nextIndex = index, error = "Unknown option '$token'.")
    }

private fun parseOutputOption(args: List<String>, index: Int, state: RunOptionsState): ParsedToken {
    val value = args.getOrNull(index + 1)
    validateOutputValue(value, state.outputDir != null)?.let { error ->
        return ParsedToken(nextIndex = index, error = error)
    }

    state.outputDir = Path.of(requireNotNull(value))
    return ParsedToken(nextIndex = index + 2)
}

private fun validateOutputValue(value: String?, outputDirAlreadySet: Boolean): String? = when {
    value == null || value.startsWith("--") -> "Missing value for --out option."
    outputDirAlreadySet -> "--out option may only be specified once."
    else -> null
}

private fun parseVariableOption(args: List<String>, index: Int, state: RunOptionsState): ParsedToken {
    val value = args.getOrNull(index + 1)
    val parsedVariable = parseVariableValue(value)
    parsedVariable.error?.let { error ->
        return ParsedToken(nextIndex = index, error = error)
    }
    if (state.variables.containsKey(parsedVariable.key)) {
        return ParsedToken(nextIndex = index, error = "--var '${parsedVariable.key}' may only be specified once.")
    }

    state.variables[parsedVariable.key] = parsedVariable.value
    return ParsedToken(nextIndex = index + 2)
}

private fun parseFlagOption(args: List<String>, index: Int, state: RunOptionsState): ParsedToken {
    val value = args.getOrNull(index + 1)
    val flag =
        parseFlagValue(value) ?: return ParsedToken(nextIndex = index, error = "Missing value for --flag option.")
    if (state.flags.contains(flag)) {
        return ParsedToken(nextIndex = index, error = "--flag '$flag' may only be specified once.")
    }

    state.flags.add(flag)
    return ParsedToken(nextIndex = index + 2)
}

private fun parsePluginOption(args: List<String>, index: Int, state: RunOptionsState): ParsedToken {
    val value = args.getOrNull(index + 1)
    val pluginCoordinate = parsePluginCoordinate(value) ?: return ParsedToken(
        nextIndex = index,
        error = "Invalid --plugin value '$value'. Expected group:artifact:version.",
    )
    if (state.plugins.contains(pluginCoordinate)) {
        return ParsedToken(
            nextIndex = index,
            error = "--plugin '$pluginCoordinate' may only be specified once.",
        )
    }

    state.plugins.add(pluginCoordinate)
    return ParsedToken(nextIndex = index + 2)
}

private fun parsePluginJarOption(args: List<String>, index: Int, state: RunOptionsState): ParsedToken {
    val value = args.getOrNull(index + 1)
    val pluginJar = value?.takeUnless { it.startsWith("--") }?.let(Path::of)
    if (pluginJar == null) {
        return ParsedToken(nextIndex = index, error = "Missing value for --plugin-jar option.")
    }
    if (state.pluginJars.contains(pluginJar)) {
        return ParsedToken(nextIndex = index, error = "--plugin-jar '$pluginJar' may only be specified once.")
    }

    state.pluginJars.add(pluginJar)
    return ParsedToken(nextIndex = index + 2)
}

private fun parseOfflineOption(index: Int, state: RunOptionsState): ParsedToken = parseSingleOccurrenceFlag(
    index = index,
    alreadySpecified = state.offline,
    optionName = "--offline",
) {
    state.offline = true
}

private fun parseRepositoryOption(args: List<String>, index: Int, state: RunOptionsState): ParsedToken {
    val value = args.getOrNull(index + 1)
    if (value == null || value.startsWith("--")) {
        return ParsedToken(nextIndex = index, error = "Missing value for --repository option.")
    }
    if (state.repositoryOverride != null) {
        return ParsedToken(nextIndex = index, error = "--repository may only be specified once.")
    }

    state.repositoryOverride = value
    return ParsedToken(nextIndex = index + 2)
}

private fun parseIsolationOption(args: List<String>, index: Int, state: RunOptionsState): ParsedToken {
    val value = args.getOrNull(index + 1)
    if (value == null || value.startsWith("--")) {
        return ParsedToken(nextIndex = index, error = "Missing value for --isolation option.")
    }
    val parsedMode = parseIsolationMode(value)
    val error =
        when {
            state.isolationModeSpecified -> "--isolation may only be specified once."
            parsedMode == null -> "Invalid --isolation value '$value'. Expected 'classloader' or 'process'."
            else -> null
        }
    if (error != null) {
        return ParsedToken(nextIndex = index, error = error)
    }

    state.isolationMode = requireNotNull(parsedMode)
    state.isolationModeSpecified = true
    return ParsedToken(nextIndex = index + 2)
}

private fun parseDiagnosticsOption(args: List<String>, index: Int, state: RunOptionsState): ParsedToken {
    val value = args.getOrNull(index + 1)
    if (value == null || value.startsWith("--")) {
        return ParsedToken(nextIndex = index, error = "Missing value for --diagnostics option.")
    }
    val parsedFormat = parseDiagnosticFormat(value)
    val error =
        when {
            state.diagnosticsFormatSpecified -> "--diagnostics may only be specified once."
            parsedFormat == null -> "Invalid --diagnostics value '$value'. Expected 'text' or 'json'."
            else -> null
        }
    if (error != null) {
        return ParsedToken(nextIndex = index, error = error)
    }

    state.diagnosticsFormat = requireNotNull(parsedFormat)
    state.diagnosticsFormatSpecified = true
    return ParsedToken(nextIndex = index + 2)
}

private fun parseVerboseOption(index: Int, state: RunOptionsState): ParsedToken = parseSingleOccurrenceFlag(
    index = index,
    alreadySpecified = state.verbose,
    optionName = "--verbose",
) {
    state.verbose = true
}

private fun parseEventLogOption(args: List<String>, index: Int, state: RunOptionsState): ParsedToken {
    val value = args.getOrNull(index + 1)
    if (value == null || value.startsWith("--")) {
        return ParsedToken(nextIndex = index, error = "Missing value for --event-log option.")
    }
    if (state.eventLog != null) {
        return ParsedToken(nextIndex = index, error = "--event-log may only be specified once.")
    }

    state.eventLog = Path.of(value)
    return ParsedToken(nextIndex = index + 2)
}
