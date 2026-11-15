package io.github.lmliam.microsmith.resolve.schemas.protobuf

import arrow.core.EitherNel
import arrow.core.raise.context.either
import arrow.core.raise.context.zipOrAccumulate
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.ReservedSpan
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.checkNoOverlap
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.checkUniqueReservedNames

@ConsistentCopyVisibility
public data class ResolvedProtobufReservations
private constructor(val names: List<ProtoIdentifier>, val spans: List<ReservedSpan>) {
    public companion object {
        public val NONE: ResolvedProtobufReservations = ResolvedProtobufReservations(emptyList(), emptyList())

        public fun of(
            names: List<ProtoIdentifier>,
            spans: List<ReservedSpan>,
        ): EitherNel<ResolvedProtobufRejection, ResolvedProtobufReservations> = either {
            val ownNames = names.toList()
            val ownSpans = spans.toList()

            zipOrAccumulate(
                { checkUniqueReservedNames(ownNames.map(ProtoIdentifier::value)) },
                { checkNoOverlap(ownSpans.map(ReservedSpan::numbers)) },
            ) { _, _ ->
                ResolvedProtobufReservations(ownNames, ownSpans)
            }
        }
    }
}
