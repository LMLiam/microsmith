package me.liam.microsmith.gen.files

import java.nio.file.Path

data class GeneratedFile(
    val relativePath: Path,
    val contents: ByteArray
)

infix fun Path.to(contents: ByteArray) = GeneratedFile(this, contents)