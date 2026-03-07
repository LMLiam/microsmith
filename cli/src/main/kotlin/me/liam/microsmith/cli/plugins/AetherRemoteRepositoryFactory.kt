package me.liam.microsmith.cli.plugins

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
        repository.credentials?.let { credentials ->
            builder.setAuthentication(
                AuthenticationBuilder()
                    .addUsername(credentials.username)
                    .addPassword(credentials.password)
                    .build(),
            )
        }
        return builder.build()
    }
}
