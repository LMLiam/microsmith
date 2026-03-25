package io.github.lmliam.microsmith.artifact.schemas.protobuf

import io.github.lmliam.microsmith.artifact.core.ArtifactId
import java.nio.file.Path
import kotlin.reflect.KClass

private val defaultProtoFileOutputRoot: Path = Path.of(".")

data class ProtoFileArtifactId(
    val relativePath: Path,
    val outputRoot: Path = defaultProtoFileOutputRoot,
) : ArtifactId<ProtoFileArtifact> {
    override val artifactType: KClass<ProtoFileArtifact> = ProtoFileArtifact::class
}
