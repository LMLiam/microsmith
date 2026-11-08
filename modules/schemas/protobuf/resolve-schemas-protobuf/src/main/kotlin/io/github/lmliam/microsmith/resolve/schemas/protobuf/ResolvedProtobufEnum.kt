package io.github.lmliam.microsmith.resolve.schemas.protobuf

import arrow.core.EitherNel
import arrow.core.NonEmptyList
import arrow.core.raise.context.either
import arrow.core.raise.context.zipOrAccumulate
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.ReservedSpan
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.checkEnumStartsAtZero
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.checkNoReservedNameUse
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.checkNoReservedNumberUse
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.checkUniqueEnumValueNames
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.checkUniqueEnumValueNumbers

@ConsistentCopyVisibility
data class ResolvedProtobufEnum
private constructor(
    override val name: ProtoIdentifier,
    val values: NonEmptyList<ResolvedProtobufEnumValue>,
    val reservations: ResolvedProtobufReservations,
) : ResolvedProtobufDeclaration {
    companion object {
        fun of(
            name: ProtoIdentifier,
            values: NonEmptyList<ResolvedProtobufEnumValue>,
            reservations: ResolvedProtobufReservations,
        ): EitherNel<ResolvedProtobufRejection, ResolvedProtobufEnum> = either {
            val valueNames = values.map { it.name.value }
            val valueNumbers = values.map { it.number.value }

            zipOrAccumulate(
                { checkEnumStartsAtZero(values.head.name.value, values.head.number.value) },
                { checkUniqueEnumValueNames(valueNames) },
                { checkUniqueEnumValueNumbers(valueNumbers) },
                { checkNoReservedNameUse(reservations.names.map(ProtoIdentifier::value), valueNames) },
                { checkNoReservedNumberUse(reservations.spans.map(ReservedSpan::numbers), valueNumbers) },
            ) { _, _, _, _, _ ->
                ResolvedProtobufEnum(name, values, reservations)
            }
        }
    }
}
