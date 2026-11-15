package io.github.lmliam.microsmith.gen.files

import java.nio.file.Path

public sealed interface FileSpace {
    public val root: Path
}
