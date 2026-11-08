package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution.ProtobufDeclarationKind

data class ResolvedProtobufReference(val target: QualifiedSchemaName, val kind: ProtobufDeclarationKind)
