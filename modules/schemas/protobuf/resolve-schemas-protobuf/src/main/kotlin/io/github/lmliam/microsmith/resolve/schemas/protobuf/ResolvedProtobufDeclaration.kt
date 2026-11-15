package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier

public sealed interface ResolvedProtobufDeclaration {
    public val name: ProtoIdentifier
}
