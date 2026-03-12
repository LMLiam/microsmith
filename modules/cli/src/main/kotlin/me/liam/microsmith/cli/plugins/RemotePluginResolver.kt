package me.liam.microsmith.cli.plugins

import java.nio.file.Path

internal interface RemotePluginResolver {
    fun resolve(
        coordinate: Coordinate,
        repositories: List<RepositoryEndpoint>,
        cacheDirectory: Path,
        offline: Boolean,
    ): ResolvedRemotePlugin
}
