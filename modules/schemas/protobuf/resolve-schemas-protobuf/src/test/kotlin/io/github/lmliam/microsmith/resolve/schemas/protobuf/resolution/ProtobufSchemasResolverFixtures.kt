package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

internal fun protobufSchemasResolver() =
    ProtobufSchemasResolver(
        ProtobufDeclarationResolver.Factory { symbols ->
            ProtobufDeclarationResolver(symbols, ProtobufReferenceResolver.Factory(::ProtobufReferenceResolver))
        },
    )
