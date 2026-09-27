package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission

import io.github.lmliam.microsmith.artifact.schemas.protobuf.render.renderDeclaration
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufDeclaration
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnum
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufMessage

internal fun validateProtobufDeclaration(declaration: ResolvedProtobufDeclaration) {
    when (declaration) {
        is ResolvedProtobufMessage -> validateMessage(declaration)
        is ResolvedProtobufEnum -> validateEnum(declaration)
    }
}

internal fun renderProtobufDeclaration(declaration: ResolvedProtobufDeclaration): String = when (declaration) {
    is ResolvedProtobufMessage -> renderDeclaration(declaration)
    is ResolvedProtobufEnum -> renderDeclaration(declaration)
}
