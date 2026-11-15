package io.github.lmliam.microsmith.gen.files

import java.nio.file.Path

class DirectorySpace(root: Path) : FileSpace {
    override val root: Path = root.toAbsolutePath().normalize()
}
