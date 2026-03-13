package io.github.lmliam.microsmith.cli.plugins

import java.io.IOException
import java.io.UncheckedIOException

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
    throw error.toAuthenticationDiagnostic()
} catch (error: IOException) {
    throw error.toAuthenticationDiagnostic()
} catch (error: UncheckedIOException) {
    throw error.toAuthenticationDiagnostic()
} catch (error: SecurityException) {
    throw error.toAuthenticationDiagnostic()
}

private fun Throwable.toAuthenticationDiagnostic(): PluginResolutionDiagnosticException =
    PluginResolutionDiagnosticException(
        category = PluginResolverErrorCategory.AUTHENTICATION,
        message = message ?: "Repository credentials configuration is invalid.",
        cause = this,
    )
