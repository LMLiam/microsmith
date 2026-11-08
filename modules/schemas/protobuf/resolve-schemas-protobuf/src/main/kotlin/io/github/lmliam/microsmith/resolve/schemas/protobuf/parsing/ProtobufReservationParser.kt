package io.github.lmliam.microsmith.resolve.schemas.protobuf.parsing

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.raise.Raise
import arrow.core.raise.RaiseAccumulate
import arrow.core.raise.context.bindNel
import arrow.core.raise.context.either
import arrow.core.raise.context.mapOrAccumulate
import arrow.core.raise.context.zipOrAccumulate
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.Reserved
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedIndex
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedName
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedRange
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedToMax
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufReservations
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.ReservedSpan
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.checkNoOverlap
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.checkNoReservedNameUse
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.checkNoReservedNumberUse
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.checkUniqueReservedNames

internal fun parseReservations(
    schemaName: String,
    reservations: List<Reserved>,
    domain: ReservedSpan.Domain,
    usedNames: List<String>,
    usedNumbers: List<Int>,
): EitherNel<ProtobufResolutionIssue, ResolvedProtobufReservations> = either {
    zipOrAccumulate(
        { parseReservedNames(schemaName, reservations.filterIsInstance<ReservedName>(), usedNames).bindNel() },
        { parseReservedSpans(schemaName, reservations, domain).bindNel() },
        { checkReservedNumbers(schemaName, reservations, domain, usedNumbers).bindNel() },
    ) { names, spans, _ ->
        ResolvedProtobufReservations(names, spans)
    }
}

private fun parseReservedNames(
    schemaName: String,
    reservations: List<ReservedName>,
    usedNames: List<String>,
): EitherNel<ProtobufResolutionIssue, List<ProtoIdentifier>> = either {
    val names = reservations.map(ReservedName::name)

    zipOrAccumulate(
        { parseReservedIdentifiers(schemaName, names) },
        { checking(schemaName) { checkUniqueReservedNames(names) } },
        { checkNoUsedNames(schemaName, names, usedNames) },
    ) { identifiers, _, _ ->
        identifiers
    }
}

context(_: RaiseAccumulate<ProtobufResolutionIssue>)
private fun parseReservedIdentifiers(schemaName: String, names: List<String>): List<ProtoIdentifier> =
    names.mapOrAccumulate {
        parseIdentifier(schemaName, ProtobufResolutionIssue.IdentifierLocation.Reservation(it), it)
    }

context(_: Raise<ProtobufResolutionIssue>)
private fun checkNoUsedNames(schemaName: String, reservedNames: List<String>, usedNames: List<String>) {
    checking(schemaName) { checkNoReservedNameUse(reservedNames, usedNames) }
}

private fun parseReservedSpans(
    schemaName: String,
    reservations: List<Reserved>,
    domain: ReservedSpan.Domain,
): EitherNel<ProtobufResolutionIssue, List<ReservedSpan>> = either {
    reservations.mapOrAccumulate { parseReservedSpan(schemaName, it, domain).bindNel() }.flatten()
}

private fun parseReservedSpan(
    schemaName: String,
    reservation: Reserved,
    domain: ReservedSpan.Domain,
): EitherNel<ProtobufResolutionIssue, List<ReservedSpan>> =
    when (reservation) {
        is ReservedName -> Either.Right(emptyList())

        is ReservedIndex ->
            ReservedSpan.single(reservation.index, domain).asIssues(schemaName, reservation.index.toString())

        is ReservedRange ->
            ReservedSpan.range(reservation.indexRange.first, reservation.indexRange.last, domain)
                .asIssues(schemaName, reservation.indexRange.toString())

        is ReservedToMax ->
            ReservedSpan.toMax(reservation.from, domain).asIssues(schemaName, "${reservation.from} to max")
    }

private fun EitherNel<ReservedSpan.Rejection, ReservedSpan>.asIssues(
    schemaName: String,
    description: String,
): EitherNel<ProtobufResolutionIssue, List<ReservedSpan>> = mapLeft { rejections ->
    rejections.map { rejection ->
        when (rejection) {
            is ReservedSpan.Rejection.Descending ->
                ProtobufResolutionIssue.InvalidReservationRange(schemaName, rejection.first, rejection.last)

            is ReservedSpan.Rejection.OutOfDomain ->
                ProtobufResolutionIssue.InvalidFieldNumber(
                    schemaName,
                    ProtobufResolutionIssue.FieldLocation.Reservation(description),
                    rejection.number,
                )
        }
    }
}
    .map(::listOf)

private fun checkReservedNumbers(
    schemaName: String,
    reservations: List<Reserved>,
    domain: ReservedSpan.Domain,
    usedNumbers: List<Int>,
): EitherNel<ProtobufResolutionIssue, Unit> = either {
    val occupied = reservations.flatMap { numbersClaimedEvenIfInvalid(it, domain) }

    zipOrAccumulate(
        { checkNoOverlap(schemaName, occupied) },
        { checkNoUsedNumbers(schemaName, occupied, usedNumbers) },
    ) { _, _ ->
    }
}

private fun numbersClaimedEvenIfInvalid(reservation: Reserved, domain: ReservedSpan.Domain): List<IntRange> =
    when (reservation) {
        is ReservedName -> emptyList()
        is ReservedIndex -> listOf(reservation.index..reservation.index)
        is ReservedRange -> listOf(reservation.indexRange).filter { it.first <= it.last }
        is ReservedToMax -> listOf(reservation.from..domain.max)
    }

context(_: Raise<ProtobufResolutionIssue>)
private fun checkNoOverlap(schemaName: String, occupied: List<IntRange>) {
    checking(schemaName) { checkNoOverlap(occupied) }
}

context(_: Raise<ProtobufResolutionIssue>)
private fun checkNoUsedNumbers(schemaName: String, occupied: List<IntRange>, usedNumbers: List<Int>) {
    checking(schemaName) { checkNoReservedNumberUse(occupied, usedNumbers) }
}
