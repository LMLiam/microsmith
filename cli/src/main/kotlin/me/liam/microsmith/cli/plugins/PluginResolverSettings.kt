package me.liam.microsmith.cli.plugins

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
