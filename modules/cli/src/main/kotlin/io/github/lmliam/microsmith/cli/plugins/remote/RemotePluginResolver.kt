package io.github.lmliam.microsmith.cli.plugins.remote

import io.github.lmliam.microsmith.cli.plugins.repository.RepositoryEndpoint
import java.nio.file.Path

internal interface RemotePluginResolver {
    fun resolve(
        coordinate: Coordinate,
        repositories: List<RepositoryEndpoint>,
        cacheDirectory: Path,
        offline: Boolean,
    ): ResolvedRemotePlugin
}
