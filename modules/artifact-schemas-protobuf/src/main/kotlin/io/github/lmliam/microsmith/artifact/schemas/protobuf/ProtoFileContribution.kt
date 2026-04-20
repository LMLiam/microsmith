package io.github.lmliam.microsmith.artifact.schemas.protobuf

import io.github.lmliam.microsmith.artifact.core.ArtifactContribution

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
