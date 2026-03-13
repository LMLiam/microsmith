package io.github.lmliam.microsmith.cli.plugins

import io.github.lmliam.microsmith.cli.command.RunCommand

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

        val repositoryPolicy = settings.repositoryPolicy ?: defaultRepositoryAllowlistPolicy()
        val repositoryUris = resolveRepositoryUris(command, settings, repositoryPolicy)
        return repositoryUris.map { repositoryUri ->
            RepositoryEndpoint(
                uri = repositoryUri,
                credentials = settings.repositoryCredentialsResolver.resolveWithDiagnostics(repositoryUri),
            )
        }
    }

    private fun resolveRepositoryUris(
        command: RunCommand,
        settings: PluginResolverSettings,
        repositoryPolicy: RepositoryAllowlistPolicy,
    ): List<String> = try {
        val override = command.repositoryOverride?.trim()?.takeIf(String::isNotEmpty)
        val repositories =
            (listOfNotNull(override) + settings.defaultRepositories)
                .map(::normalizeRepositoryUri)
                .distinct()
        repositories.forEach(repositoryPolicy::validate)
        repositories
    } catch (error: IllegalArgumentException) {
        throw PluginResolutionDiagnosticException(
            category = PluginResolverErrorCategory.REPOSITORY_POLICY,
            message = error.message ?: "Repository configuration was rejected by policy.",
            cause = error,
        )
    }
}
