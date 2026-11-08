package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

internal data class SchemaNode(val identity: QualifiedSchemaName, val dependencies: List<QualifiedSchemaName>)
