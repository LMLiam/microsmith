package me.liam.microsmith.cli.init

import me.liam.microsmith.cli.ide.IdeHelperRefreshResult
import java.nio.file.Path

internal data class InitBootstrapResult(
    val projectRoot: Path,
    val repositoryDetection: OnboardingRepositoryDetection,
    val createdFiles: List<Path>,
    val overwrittenFiles: List<Path>,
    val preservedFiles: List<Path>,
    val ideHelperResult: IdeHelperRefreshResult?,
)
