package io.github.lmliam.microsmith.gen.files

import java.nio.file.Path

public class DirectorySpace(root: Path) : FileSpace {
    override val root: Path = root.toAbsolutePath().normalize()
}
