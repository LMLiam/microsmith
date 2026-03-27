package io.github.lmliam.microsmith.artifact.files

import io.github.lmliam.microsmith.artifact.core.ArtifactId
import java.nio.file.Path
import kotlin.reflect.KClass

private val defaultTextFileOutputRoot: Path = Path.of(".")

data class TextFileArtifactId(
    val relativePath: Path,
    val outputRoot: Path = defaultTextFileOutputRoot,
) : ArtifactId<TextFileArtifact> {
    override val artifactType: KClass<TextFileArtifact> = TextFileArtifact::class
}
