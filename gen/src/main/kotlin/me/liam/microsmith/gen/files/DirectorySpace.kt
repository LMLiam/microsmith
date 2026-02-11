package me.liam.microsmith.gen.files

import java.nio.file.Files
import java.nio.file.Path

class DirectorySpace private constructor(
    override val root: Path,
) : FileSpace {
    companion object {
        fun from(path: Path): DirectorySpace {
            val normalizedRoot = path.toAbsolutePath().normalize()
            Files.createDirectories(normalizedRoot)
            return DirectorySpace(normalizedRoot)
        }
    }
}
