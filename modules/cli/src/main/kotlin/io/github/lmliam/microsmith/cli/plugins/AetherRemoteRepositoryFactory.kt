package io.github.lmliam.microsmith.cli.plugins

import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.util.repository.AuthenticationBuilder

private const val REMOTE_REPOSITORY_ID_PREFIX = "repo"
private const val REMOTE_REPOSITORY_TYPE_DEFAULT = "default"

internal class AetherRemoteRepositoryFactory {
    fun create(index: Int, repository: RepositoryEndpoint): RemoteRepository {
        val builder = RemoteRepository.Builder(
            "$REMOTE_REPOSITORY_ID_PREFIX-$index",
            REMOTE_REPOSITORY_TYPE_DEFAULT,
            repository.uri,
        )
        repository.credentials?.let(builder::applyAuthentication)
        return builder.build()
    }
}

@Suppress("UsePropertyAccessSyntax")
private fun RemoteRepository.Builder.applyAuthentication(credentials: RepositoryCredentials) {
    /*
     * Kotlin property syntax is not usable here: the Java type exposes a package-private field and
     * the builder setter remains the only accessible API from this module.
     */
    setAuthentication(
        AuthenticationBuilder()
            .addUsername(credentials.username)
            .addPassword(credentials.password)
            .build(),
    )
}
