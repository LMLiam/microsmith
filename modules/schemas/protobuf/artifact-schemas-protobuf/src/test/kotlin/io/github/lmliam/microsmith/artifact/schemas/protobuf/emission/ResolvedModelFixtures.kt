package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission

import arrow.core.NonEmptyList
import arrow.core.getOrElse
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnum
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnumValue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufReservations
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.ReservedSpan

internal fun reservations(
    names: List<ProtoIdentifier> = emptyList(),
    spans: List<ReservedSpan> = emptyList(),
): ResolvedProtobufReservations =
    ResolvedProtobufReservations.of(names, spans).getOrElse { error("invalid reservations: $it") }

internal fun enumDeclaration(
    name: String,
    values: NonEmptyList<ResolvedProtobufEnumValue>,
    reservations: ResolvedProtobufReservations = ResolvedProtobufReservations.NONE,
): ResolvedProtobufEnum =
    ResolvedProtobufEnum.of(protoIdentifier(name), values, reservations).getOrElse { error("invalid enum: $it") }
