package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.Reserved

data class ResolvedProtobufMessage(
    override val name: String,
    val fields: List<ResolvedProtobufField>,
    val oneofs: List<ResolvedProtobufOneof>,
    val reservations: List<Reserved>,
) : ResolvedProtobufDeclaration
