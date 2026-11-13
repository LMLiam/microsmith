package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Enum
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.ServiceDeclaration
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Type

enum class ProtobufDeclarationKind {
    MESSAGE,
    ENUM,
    SERVICE,
}

fun Type.declarationKind(): ProtobufDeclarationKind =
    when (this) {
        is Message -> ProtobufDeclarationKind.MESSAGE
        is Enum -> ProtobufDeclarationKind.ENUM
        is ServiceDeclaration -> ProtobufDeclarationKind.SERVICE
    }
