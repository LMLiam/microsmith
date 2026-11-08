package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.ReservedSpan

data class ResolvedProtobufReservations(
    val names: List<ProtoIdentifier> = emptyList(),
    val spans: List<ReservedSpan> = emptyList(),
)
