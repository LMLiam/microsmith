package io.github.lmliam.microsmith.artifact.schemas.protobuf

import io.github.lmliam.microsmith.artifact.schemas.protobuf.core.ProtobufArtifact

data class ProtoFileArtifact(
    override val id: ProtoFileArtifactId,
    val packageName: String?,
    val imports: List<String>,
    val declarations: List<ProtoDeclaration>,
    val origins: Set<String> = emptySet(),
) : ProtobufArtifact
