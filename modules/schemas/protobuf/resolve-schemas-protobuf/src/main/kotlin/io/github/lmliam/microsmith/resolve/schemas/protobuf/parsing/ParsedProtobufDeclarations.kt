package io.github.lmliam.microsmith.resolve.schemas.protobuf.parsing

import arrow.core.NonEmptyList
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufReservations
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.EnumValueNumber
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.FieldNumber

internal data class ParsedMessage(
    val fields: List<ParsedField>,
    val oneofs: List<ParsedOneof>,
    val reservations: ResolvedProtobufReservations,
)

internal data class ParsedField(val name: ProtoIdentifier, val number: FieldNumber)

internal data class ParsedOneof(val name: ProtoIdentifier, val fields: NonEmptyList<ParsedField>)

internal data class ParsedEnum(
    val values: NonEmptyList<ParsedEnumValue>,
    val reservations: ResolvedProtobufReservations,
)

internal data class ParsedEnumValue(val name: ProtoIdentifier, val number: EnumValueNumber)
