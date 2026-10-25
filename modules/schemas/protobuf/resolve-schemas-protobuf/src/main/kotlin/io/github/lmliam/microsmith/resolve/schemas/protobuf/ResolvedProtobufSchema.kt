package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

data class ResolvedProtobufSchema(
    val identity: QualifiedSchemaName,
    val declaration: ResolvedProtobufDeclaration,
    val dependencies: List<QualifiedSchemaName>,
)
