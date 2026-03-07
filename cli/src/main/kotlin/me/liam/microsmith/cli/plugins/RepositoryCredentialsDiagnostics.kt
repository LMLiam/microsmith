package me.liam.microsmith.cli.plugins

internal fun RepositoryCredentialsResolver.resolveWithDiagnostics(repositoryUri: String): RepositoryCredentials? =
    runWithCredentialDiagnostics {
        resolve(repositoryUri)
    }

internal fun RepositoryCredentialsResolver.sensitiveValuesWithDiagnostics(): Set<String> =
    runWithCredentialDiagnostics {
        sensitiveValues()
    }

private inline fun <T> runWithCredentialDiagnostics(block: () -> T): T = try {
    block()
} catch (error: IllegalArgumentException) {
    throw PluginResolutionDiagnosticException(
        category = PluginResolverErrorCategory.AUTHENTICATION,
        message = error.message ?: "Repository credentials configuration is invalid.",
        cause = error,
    )
}
