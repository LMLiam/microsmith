package io.github.lmliam.microsmith.artifact.files

public class GeneratedFile(
    public val relativePath: RelativePath,
    contents: ByteArray,
    public val outputRoot: RelativePath = RelativePath.ROOT,
) {
    init {
        require(!relativePath.isRoot) { "Relative path must name a file." }
    }

    private val bytes = contents.copyOf()

    public val contents: ByteArray
        get() = bytes.copyOf()
}
