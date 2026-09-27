package io.github.lmliam.microsmith.cli.plugins.cache

import io.github.lmliam.microsmith.cli.plugins.remote.Coordinate
import java.nio.file.Path

internal fun pluginArtifactCacheRoot(cacheDirectory: Path): Path = cacheDirectory
    .resolve("artifacts")
    .toAbsolutePath()
    .normalize()

internal fun cachePathFor(cacheRoot: Path, coordinate: Coordinate): Path {
    val artifactPath =
        cacheRoot
            .resolve(coordinate.group.replace('.', '/'))
            .resolve(coordinate.artifact)
            .resolve(coordinate.version)
            .resolve("${coordinate.artifact}-${coordinate.version}.jar")
            .normalize()
    require(artifactPath.startsWith(cacheRoot)) {
        "Plugin coordinate '${coordinate.value}' resolves outside plugin cache directory."
    }
    return artifactPath
}
