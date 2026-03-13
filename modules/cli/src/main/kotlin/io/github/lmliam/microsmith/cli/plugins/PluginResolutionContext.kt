package io.github.lmliam.microsmith.cli.plugins

import java.nio.file.Path

internal data class PluginResolutionContext(
    val lockfilePath: Path,
    val lockfile: ParsedLockfile?,
    val checksumAllowlist: PluginChecksumAllowlist?,
    val cacheDirectory: Path,
    val repositories: List<RepositoryEndpoint>,
)
