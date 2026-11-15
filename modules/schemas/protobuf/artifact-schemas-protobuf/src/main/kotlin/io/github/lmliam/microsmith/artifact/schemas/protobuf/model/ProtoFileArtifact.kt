package io.github.lmliam.microsmith.artifact.schemas.protobuf.model

import io.github.lmliam.microsmith.artifact.schemas.protobuf.ProtobufArtifact

public data class ProtoFileArtifact(
    public override val id: ProtoFileArtifactId,
    public val imports: List<String>,
    public val declarations: List<ProtoDeclaration>,
) : ProtobufArtifact
