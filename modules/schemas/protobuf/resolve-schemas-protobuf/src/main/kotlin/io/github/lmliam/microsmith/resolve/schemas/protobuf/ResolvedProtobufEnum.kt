package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier

data class ResolvedProtobufEnum(
    override val name: ProtoIdentifier,
    val values: List<ResolvedProtobufEnumValue>,
    val reservations: ResolvedProtobufReservations,
) : ResolvedProtobufDeclaration
