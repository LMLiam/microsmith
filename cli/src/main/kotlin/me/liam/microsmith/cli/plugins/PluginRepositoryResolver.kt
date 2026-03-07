package me.liam.microsmith.cli.plugins

import me.liam.microsmith.cli.command.RunCommand

/** Resolves repository endpoints while keeping allowlist policy and credential lookup at the boundary. */
internal class PluginRepositoryResolver {
    fun resolve(
        command: RunCommand,
        settings: PluginResolverSettings,
        requiresRemoteRepositories: Boolean,
    ): List<RepositoryEndpoint> {
        if (!requiresRemoteRepositories) {
            return emptyList()
        }

        return try {
            val repositoryPolicy = settings.repositoryPolicy ?: defaultRepositoryAllowlistPolicy()
            resolveRepositoryUris(command, settings, repositoryPolicy)
                .map { repositoryUri ->
                    RepositoryEndpoint(
                        uri = repositoryUri,
                        credentials = settings.repositoryCredentialsResolver.resolve(repositoryUri),
                    )
                }
        } catch (error: IllegalArgumentException) {
            throw PluginResolutionDiagnosticException(
                category = PluginResolverErrorCategory.REPOSITORY_POLICY,
                message = error.message ?: "Repository configuration was rejected by policy.",
                cause = error,
            )
        }
    }

    private fun resolveRepositoryUris(
        command: RunCommand,
        settings: PluginResolverSettings,
        repositoryPolicy: RepositoryAllowlistPolicy,
    ): List<String> {
        val override = command.repositoryOverride?.trim()?.takeIf(String::isNotEmpty)
        val repositories =
            (listOfNotNull(override) + settings.defaultRepositories)
                .map(::normalizeRepositoryUri)
                .distinct()
        repositories.forEach(repositoryPolicy::validate)
        return repositories
    }
}
