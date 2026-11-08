package io.github.lmliam.microsmith.cli.ide

import java.nio.file.Path

internal data class IdeHelperRefreshResult(
    val projectRoot: Path,
    val helperRoot: Path,
    val updatedFiles: List<Path>,
    val classpathEntries: List<Path>,
)
