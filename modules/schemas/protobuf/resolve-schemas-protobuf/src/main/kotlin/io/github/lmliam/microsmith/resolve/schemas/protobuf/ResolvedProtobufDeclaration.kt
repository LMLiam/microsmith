package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier

sealed interface ResolvedProtobufDeclaration {
    val name: ProtoIdentifier
}
