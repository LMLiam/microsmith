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
    if (command.plugins.isEmpty()) {
        return resolveWithDiagnostics(command, settings, diagnostics, sensitiveValues = emptySet())
    }

    val sensitiveValues =
        runCatching {
            settings.repositoryCredentialsResolver.sensitiveValuesWithDiagnostics()
        }.getOrElse { error ->
            return PluginResolutionResult.Failure(listOf(diagnostics.format(error, sensitiveValues = emptySet())))
        }

    return resolveWithDiagnostics(command, settings, diagnostics, sensitiveValues)
}

private fun RunCommand.requiresPluginResolution(): Boolean = plugins.isNotEmpty() || pluginJars.isNotEmpty()

private fun resolveWithDiagnostics(
    command: RunCommand,
    settings: PluginResolverSettings,
    diagnostics: PluginResolutionDiagnostics,
    sensitiveValues: Set<String>,
): PluginResolutionResult = runCatching {
    PluginResolutionService(settings = settings).resolve(command)
}.fold(
    onSuccess = { success -> success },
    onFailure = { error ->
        PluginResolutionResult.Failure(listOf(diagnostics.format(error, sensitiveValues)))
    },
)
