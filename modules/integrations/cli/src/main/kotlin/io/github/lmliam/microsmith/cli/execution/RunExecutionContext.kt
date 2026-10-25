package io.github.lmliam.microsmith.cli.execution

import java.nio.file.Path

internal data class RunExecutionContext(
    var resolverStatus: RunExecutionStatus = RunExecutionStatus.SKIPPED,
    var lockfilePath: Path? = null,
)
