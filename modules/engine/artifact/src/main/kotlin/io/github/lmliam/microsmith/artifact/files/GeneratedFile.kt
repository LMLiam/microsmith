package io.github.lmliam.microsmith.artifact.files

class GeneratedFile(
    val relativePath: RelativePath,
    contents: ByteArray,
    val outputRoot: RelativePath = RelativePath.ROOT,
) {
    init {
        require(!relativePath.isRoot) { "Relative path must name a file." }
    }

    private val bytes = contents.copyOf()

    val contents: ByteArray
        get() = bytes.copyOf()
}

infix fun RelativePath.to(contents: ByteArray): GeneratedFile = GeneratedFile(this, contents)
