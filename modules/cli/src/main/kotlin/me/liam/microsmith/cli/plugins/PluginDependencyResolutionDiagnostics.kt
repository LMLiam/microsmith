package me.liam.microsmith.cli.plugins

import org.eclipse.aether.resolution.DependencyResolutionException
import org.eclipse.aether.transfer.ArtifactNotFoundException

internal class PluginDependencyResolutionDiagnostics {
    fun toDiagnostic(
        coordinate: Coordinate,
        localRepositoryRoot: java.nio.file.Path,
        repositories: List<String>,
        offline: Boolean,
        error: DependencyResolutionException,
    ): PluginResolutionDiagnosticException {
        val cause = primaryResolutionCause(error)
        val repositoryList = repositories.joinToString(", ")
        val authenticationFailure = isAuthenticationFailure(cause)
        val reason =
            when (cause) {
                is ArtifactNotFoundException ->
                    "Artifact '${cause.artifact}' was not found in configured repositories."

                else -> cause?.message ?: error.message ?: "Unknown resolver failure."
            }

        val remediation =
            if (authenticationFailure) {
                "Verify repository credentials. Configure per-endpoint credentials via " +
                    "$REPOSITORY_CREDENTIALS_FILE_ENV, global credentials via " +
                    "$REPOSITORY_USERNAME_ENV/$REPOSITORY_PASSWORD_ENV, or GitHub Packages via " +
                    "$GITHUB_PACKAGES_USERNAME_ENV/$GITHUB_PACKAGES_TOKEN_ENV."
            } else if (offline) {
                "Offline mode is enabled. Ensure the full dependency graph is cached under " +
                    "'$localRepositoryRoot' by running once without --offline."
            } else {
                "Verify plugin coordinates and repository availability."
            }

        val category =
            if (authenticationFailure) {
                PluginResolverErrorCategory.AUTHENTICATION
            } else {
                PluginResolverErrorCategory.DEPENDENCY_RESOLUTION
            }

        return PluginResolutionDiagnosticException(
            category = category,
            message =
            "Could not resolve plugin '${coordinate.value}' with transitive dependencies. " +
                "Repositories: $repositoryList. $reason $remediation",
            cause = error,
        )
    }

    private fun isAuthenticationFailure(cause: Throwable?): Boolean {
        if (cause == null) {
            return false
        }

        return generateSequence(cause) { throwable -> throwable.cause }
            .mapNotNull(Throwable::message)
            .map(String::lowercase)
            .any { message ->
                message.contains("status code: 401") ||
                    message.contains("status code: 403") ||
                    message.contains("unauthorized") ||
                    message.contains("forbidden") ||
                    message.contains("authentication failed") ||
                    message.contains("not authorized")
            }
    }

    private fun primaryResolutionCause(error: DependencyResolutionException): Throwable? =
        error.result.collectExceptions.firstOrNull()
            ?: error.result.artifactResults
                .asSequence()
                .flatMap { artifactResult -> artifactResult.exceptions.asSequence() }
                .firstOrNull()
            ?: error.cause
}
