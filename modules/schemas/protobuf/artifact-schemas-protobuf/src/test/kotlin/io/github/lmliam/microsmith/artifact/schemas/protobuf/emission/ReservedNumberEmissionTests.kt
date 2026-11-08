package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission

import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.Reserved
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedIndex
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedRange
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedToMax
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufMessage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec

class ReservedNumberEmissionTests :
    StringSpec({
        fun messageReserving(vararg reservations: Reserved) =
            ResolvedProtobufMessage(
                name = protoIdentifier("Contact"),
                fields = emptyList(),
                oneofs = emptyList(),
                reservations = reservations.toList(),
            )

        "message reservations accept a single number in the implementation range" {
            validateProtobufDeclaration(messageReserving(ReservedIndex(19_500)))
        }

        "message reservations accept a range that straddles the implementation range" {
            validateProtobufDeclaration(messageReserving(ReservedRange(18_000..20_000)))
        }

        "message reservations accept a range that is exactly the implementation range" {
            validateProtobufDeclaration(messageReserving(ReservedRange(19_000..19_999)))
        }

        "message reservations accept a range to max that starts in the implementation range" {
            validateProtobufDeclaration(messageReserving(ReservedToMax(19_500)))
        }

        "message reservations reject zero" {
            shouldThrow<IllegalArgumentException> { validateProtobufDeclaration(messageReserving(ReservedIndex(0))) }
        }

        "message reservations reject a number above the maximum field number" {
            shouldThrow<IllegalArgumentException> {
                validateProtobufDeclaration(messageReserving(ReservedIndex(536_870_912)))
            }
        }
    })
