package me.liam.microsmith.gen.files

import java.nio.file.Path

sealed interface FileSpace {
    val root: Path
    fun resolve(relative: String) = root.resolve(relative)
}

data class TemporaryFileSpace(override val root: Path) : FileSpace
data class SourceFileSpace(override val root: Path) : FileSpace