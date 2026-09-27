package io.github.lmliam.microsmith.artifact.schemas.protobuf.contribution
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.schemas.protobuf.model.ProtoDeclaration
import io.github.lmliam.microsmith.artifact.schemas.protobuf.model.ProtoFileArtifact
import io.github.lmliam.microsmith.artifact.schemas.protobuf.model.ProtoFileArtifactId

data class ProtoFileContribution(
    override val artifactId: ProtoFileArtifactId,
    val packageName: String?,
    val imports: List<String> = emptyList(),
    val declarations: List<ProtoDeclaration>,
    val origins: Set<String> = emptySet(),
) : ArtifactContribution<ProtoFileArtifact> {
    init {
        require(declarations.isNotEmpty()) {
            "Proto file contributions must declare at least one top-level declaration."
        }
    }
}
