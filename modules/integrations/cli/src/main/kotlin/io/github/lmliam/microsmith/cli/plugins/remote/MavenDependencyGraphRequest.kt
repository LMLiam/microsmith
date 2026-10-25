package io.github.lmliam.microsmith.cli.plugins.remote

import java.nio.file.Path
import org.eclipse.aether.repository.RemoteRepository

internal data class MavenDependencyGraphRequest(
    val coordinate: Coordinate,
    val repositories: List<RemoteRepository>,
    val localRepositoryRoot: Path,
    val offline: Boolean,
)
