package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.Reserved

data class ResolvedProtobufEnum(
    override val name: String,
    val values: List<ResolvedProtobufEnumValue>,
    val reservations: List<Reserved>,
) : ResolvedProtobufDeclaration
