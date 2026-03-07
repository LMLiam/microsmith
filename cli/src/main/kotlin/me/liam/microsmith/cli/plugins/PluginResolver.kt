package me.liam.microsmith.cli.plugins

import me.liam.microsmith.cli.command.RunCommand

internal fun resolvePlugins(command: RunCommand): PluginResolutionResult {
    if (!command.requiresPluginResolution()) {
        return PluginResolutionResult.Success(classpath = emptyList(), lockfilePath = null)
    }

    return runCatching {
        PluginResolverSettings()
    }.fold(
        onSuccess = { settings -> resolvePlugins(command = command, settings = settings) },
        onFailure = { error ->
            val diagnostics = PluginResolutionDiagnostics()
            PluginResolutionResult.Failure(listOf(diagnostics.format(error, sensitiveValues = emptySet())))
        },
    )
}

internal fun resolvePlugins(command: RunCommand, settings: PluginResolverSettings): PluginResolutionResult {
    if (!command.requiresPluginResolution()) {
        return PluginResolutionResult.Success(classpath = emptyList(), lockfilePath = null)
    }

    val diagnostics = PluginResolutionDiagnostics()
    var sensitiveValues: Set<String> = emptySet()

    return runCatching {
        if (command.plugins.isNotEmpty()) {
            sensitiveValues = settings.repositoryCredentialsResolver.sensitiveValuesWithDiagnostics()
        }
        PluginResolutionService(settings = settings).resolve(command)
    }.fold(
        onSuccess = { success -> success },
        onFailure = { error ->
            PluginResolutionResult.Failure(listOf(diagnostics.format(error, sensitiveValues)))
        },
    )
}

private fun RunCommand.requiresPluginResolution(): Boolean = plugins.isNotEmpty() || pluginJars.isNotEmpty()
