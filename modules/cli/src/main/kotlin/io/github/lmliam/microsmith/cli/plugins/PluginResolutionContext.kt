package io.github.lmliam.microsmith.cli.plugins

import io.github.lmliam.microsmith.cli.plugins.integrity.PluginChecksumAllowlist
import io.github.lmliam.microsmith.cli.plugins.lockfile.ParsedLockfile
import io.github.lmliam.microsmith.cli.plugins.repository.RepositoryEndpoint
import java.nio.file.Path

internal data class PluginResolutionContext(
    val lockfilePath: Path,
    val lockfile: ParsedLockfile?,
    val checksumAllowlist: PluginChecksumAllowlist?,
    val cacheDirectory: Path,
    val repositories: List<RepositoryEndpoint>,
)
