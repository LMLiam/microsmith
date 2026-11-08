package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission

import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufMessage
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufReservations
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.ReservedSpan
import io.kotest.core.spec.style.StringSpec

class ReservedNumberEmissionTests :
    StringSpec({
        fun messageReserving(vararg spans: ReservedSpan) =
            ResolvedProtobufMessage(
                name = protoIdentifier("Contact"),
                fields = emptyList(),
                oneofs = emptyList(),
                reservations = ResolvedProtobufReservations(spans = spans.toList()),
            )

        "message reservations accept a single number in the implementation range" {
            validateProtobufDeclaration(messageReserving(singleSpan(19_500)))
        }

        "message reservations accept a range that straddles the implementation range" {
            validateProtobufDeclaration(messageReserving(rangeSpan(18_000, 20_000)))
        }

        "message reservations accept a range that is exactly the implementation range" {
            validateProtobufDeclaration(messageReserving(rangeSpan(19_000, 19_999)))
        }

        "message reservations accept a range to max that starts in the implementation range" {
            validateProtobufDeclaration(messageReserving(toMaxSpan(19_500)))
        }
    })
