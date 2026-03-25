package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

data class ResolvedProtobufSchema(
    val schema: ProtobufSchema,
    val qualifiedName: QualifiedSchemaName,
)
