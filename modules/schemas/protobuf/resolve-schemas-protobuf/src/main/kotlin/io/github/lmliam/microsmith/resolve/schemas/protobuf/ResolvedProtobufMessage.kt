package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.Reserved
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier

data class ResolvedProtobufMessage(
    override val name: ProtoIdentifier,
    val fields: List<ResolvedProtobufField>,
    val oneofs: List<ResolvedProtobufOneof>,
    val reservations: List<Reserved>,
) : ResolvedProtobufDeclaration
