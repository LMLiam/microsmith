package io.github.lmliam.microsmith.cli.init

import java.nio.file.Path

internal sealed interface BootstrapFileWriteResult {
    data class Created(val path: Path) : BootstrapFileWriteResult

    data class Overwritten(val path: Path) : BootstrapFileWriteResult

    data class Preserved(val path: Path) : BootstrapFileWriteResult
}
