package io.github.lmliam.microsmith.gen.files

import java.nio.file.Path

sealed interface FileSpace {
    val root: Path
}
