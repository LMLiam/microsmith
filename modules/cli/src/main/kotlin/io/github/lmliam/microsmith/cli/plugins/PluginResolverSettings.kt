package io.github.lmliam.microsmith.cli.plugins

import io.github.lmliam.microsmith.cli.plugins.cache.defaultPluginCacheDirectory
import io.github.lmliam.microsmith.cli.plugins.integrity.PluginChecksumAllowlist
import io.github.lmliam.microsmith.cli.plugins.remote.MavenRemotePluginResolver
import io.github.lmliam.microsmith.cli.plugins.remote.RemotePluginResolver
import io.github.lmliam.microsmith.cli.plugins.repository.RepositoryAllowlistPolicy
import io.github.lmliam.microsmith.cli.plugins.repository.RepositoryCredentialsResolver
import io.github.lmliam.microsmith.cli.plugins.repository.lazyDefaultRepositoryCredentialsResolver
import java.nio.file.Path

internal data class PluginResolverSettings(
    val cacheDirectory: Path = defaultPluginCacheDirectory(),
    val lockfilePathOverride: Path? = null,
    val defaultRepositories: List<String> = listOf(MAVEN_CENTRAL_REPOSITORY),
    val repositoryPolicy: RepositoryAllowlistPolicy? = null,
    val repositoryCredentialsResolver: RepositoryCredentialsResolver = lazyDefaultRepositoryCredentialsResolver(),
    val checksumAllowlist: PluginChecksumAllowlist? = null,
    val remotePluginResolver: RemotePluginResolver = MavenRemotePluginResolver(),
)
