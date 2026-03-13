package io.github.lmliam.microsmith.cli.plugins

import org.eclipse.aether.repository.RemoteRepository
import java.nio.file.Path

internal data class MavenDependencyGraphRequest(
    val coordinate: Coordinate,
    val repositories: List<RemoteRepository>,
    val localRepositoryRoot: Path,
    val offline: Boolean,
)
