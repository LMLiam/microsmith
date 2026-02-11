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

private fun parseRunOptionToken(args: List<String>, index: Int, state: RunOptionsState): ParsedToken {
    val token = args[index]
    return when (token) {
        OUTPUT_OPTION -> parseOutputOption(args, index, state)
        VARIABLE_OPTION -> parseVariableOption(args, index, state)
        FLAG_OPTION -> parseFlagOption(args, index, state)
        PLUGIN_OPTION -> parsePluginOption(args, index, state)
        PLUGIN_JAR_OPTION -> parsePluginJarOption(args, index, state)
        OFFLINE_OPTION -> parseOfflineOption(index, state)
        REPOSITORY_OPTION -> parseRepositoryOption(args, index, state)
        else -> ParsedToken(nextIndex = index, error = "Unknown option '$token'.")
    }
}

private fun parseOutputOption(args: List<String>, index: Int, state: RunOptionsState): ParsedToken {
    val value = args.getOrNull(index + 1)
    val error = validateOutputValue(value, state.outputDir != null)
    return if (error != null) {
        ParsedToken(nextIndex = index, error = error)
    } else {
        state.outputDir = Path.of(requireNotNull(value))
        ParsedToken(nextIndex = index + 2)
    }
}

private fun parseVariableOption(args: List<String>, index: Int, state: RunOptionsState): ParsedToken {
    val value = args.getOrNull(index + 1)
    val parsedVariable = parseVariableValue(value)
    val duplicate = parsedVariable.error == null && state.variables.containsKey(parsedVariable.key)

    return when {
        parsedVariable.error != null ->
            ParsedToken(nextIndex = index, error = parsedVariable.error)

        duplicate ->
            ParsedToken(nextIndex = index, error = "--var '${parsedVariable.key}' may only be specified once.")

        else -> {
            state.variables[parsedVariable.key] = parsedVariable.value
            ParsedToken(nextIndex = index + 2)
        }
    }
}

private fun parseFlagOption(args: List<String>, index: Int, state: RunOptionsState): ParsedToken {
    val value = args.getOrNull(index + 1)
    val flag = parseFlagValue(value)
    val duplicate = flag != null && state.flags.contains(flag)

    return when {
        flag == null ->
            ParsedToken(nextIndex = index, error = "Missing value for --flag option.")

        duplicate ->
            ParsedToken(nextIndex = index, error = "--flag '$flag' may only be specified once.")

        else -> {
            state.flags.add(flag)
            ParsedToken(nextIndex = index + 2)
        }
    }
}

private fun parsePluginOption(args: List<String>, index: Int, state: RunOptionsState): ParsedToken {
    val value = args.getOrNull(index + 1)
    val pluginCoordinate = parsePluginCoordinate(value)
    val duplicate = pluginCoordinate != null && state.plugins.contains(pluginCoordinate)

    return when {
        pluginCoordinate == null ->
            ParsedToken(
                nextIndex = index,
                error = "Invalid --plugin value '$value'. Expected group:artifact:version.",
            )

        duplicate ->
            ParsedToken(
                nextIndex = index,
                error = "--plugin '$pluginCoordinate' may only be specified once.",
            )

        else -> {
            state.plugins.add(pluginCoordinate)
            ParsedToken(nextIndex = index + 2)
        }
    }
}

private fun parsePluginJarOption(args: List<String>, index: Int, state: RunOptionsState): ParsedToken {
    val value = args.getOrNull(index + 1)
    val pluginJar = value?.takeUnless { it.startsWith("--") }?.let(Path::of)
    val duplicate = pluginJar != null && state.pluginJars.contains(pluginJar)

    return when {
        pluginJar == null ->
            ParsedToken(nextIndex = index, error = "Missing value for --plugin-jar option.")

        duplicate ->
            ParsedToken(nextIndex = index, error = "--plugin-jar '$pluginJar' may only be specified once.")

        else -> {
            state.pluginJars.add(pluginJar)
            ParsedToken(nextIndex = index + 2)
        }
    }
}

private fun parseOfflineOption(index: Int, state: RunOptionsState): ParsedToken {
    val duplicate = state.offline
    return if (duplicate) {
        ParsedToken(nextIndex = index, error = "--offline may only be specified once.")
    } else {
        state.offline = true
        ParsedToken(nextIndex = index + 1)
    }
}

private fun parseRepositoryOption(args: List<String>, index: Int, state: RunOptionsState): ParsedToken {
    val value = args.getOrNull(index + 1)
    val missingValue = value == null || value.startsWith("--")
    val duplicate = state.repositoryOverride != null

    return when {
        missingValue ->
            ParsedToken(nextIndex = index, error = "Missing value for --repository option.")

        duplicate ->
            ParsedToken(nextIndex = index, error = "--repository may only be specified once.")

        else -> {
            state.repositoryOverride = value
            ParsedToken(nextIndex = index + 2)
        }
    }
}
