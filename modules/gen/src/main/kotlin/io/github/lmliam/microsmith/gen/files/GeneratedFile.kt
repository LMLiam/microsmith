package io.github.lmliam.microsmith.gen.files

import java.nio.file.Path

class GeneratedFile(val relativePath: Path, contents: ByteArray) {
    private val bytes = contents.copyOf()

    val contents: ByteArray
        get() = bytes.copyOf()
}

infix fun Path.to(contents: ByteArray): GeneratedFile = GeneratedFile(this, contents)
