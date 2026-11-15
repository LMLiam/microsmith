package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution.ProtobufDeclarationKind

public data class ResolvedProtobufReference(
    public val target: QualifiedSchemaName,
    public val kind: ProtobufDeclarationKind,
)
