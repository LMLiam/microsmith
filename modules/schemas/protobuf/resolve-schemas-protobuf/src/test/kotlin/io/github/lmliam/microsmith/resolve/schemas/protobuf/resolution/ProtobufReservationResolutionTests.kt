package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import io.github.lmliam.microsmith.dsl.schemas.SchemasExtension
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.Reserved
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedIndex
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedRange
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedToMax
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Enum
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.EnumValue
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchemaModel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.types.shouldBeTypeOf

class ProtobufReservationResolutionTests :
    StringSpec({
        val resolver = ProtobufSchemasResolver()

        fun resolveMessage(vararg reservations: Reserved) =
            resolver.resolve(
                SchemasExtension(setOf(ProtobufSchema("User", Message("User", reserved = reservations.toList()))))
            )

        fun resolveEnum(vararg reservations: Reserved) =
            resolver.resolve(
                SchemasExtension(
                    setOf(
                        ProtobufSchema(
                            "Status",
                            Enum("Status", listOf(EnumValue("UNSPECIFIED", 0)), reservations.toList()),
                        )
                    )
                )
            )

        "message reservations accept a single number in the implementation range" {
            resolveMessage(ReservedIndex(19_500))
                .shouldBeTypeOf<DomainResolution.Success<ResolvedProtobufSchemaModel>>()
        }

        "message reservations accept a range that straddles the implementation range" {
            resolveMessage(ReservedRange(18_000..20_000))
                .shouldBeTypeOf<DomainResolution.Success<ResolvedProtobufSchemaModel>>()
        }

        "message reservations accept a range that is exactly the implementation range" {
            resolveMessage(ReservedRange(19_000..19_999))
                .shouldBeTypeOf<DomainResolution.Success<ResolvedProtobufSchemaModel>>()
        }

        "message reservations accept a range to max that starts in the implementation range" {
            resolveMessage(ReservedToMax(19_500))
                .shouldBeTypeOf<DomainResolution.Success<ResolvedProtobufSchemaModel>>()
        }

        "message reservations accept a range to max" {
            resolveMessage(ReservedToMax(1_000)).shouldBeTypeOf<DomainResolution.Success<ResolvedProtobufSchemaModel>>()
        }

        "message reservations reject zero" {
            val failure = resolveMessage(ReservedIndex(0)).shouldBeTypeOf<DomainResolution.Failure>()

            failure.issues.toList() shouldContainExactly
                listOf(
                    ProtobufResolutionIssue.InvalidFieldNumber(
                        "User",
                        ProtobufResolutionIssue.FieldLocation.Reservation("0"),
                        0,
                    )
                )
        }

        "message reservations reject a number above the maximum field number" {
            val failure = resolveMessage(ReservedIndex(536_870_912)).shouldBeTypeOf<DomainResolution.Failure>()

            failure.issues.toList() shouldContainExactly
                listOf(
                    ProtobufResolutionIssue.InvalidFieldNumber(
                        "User",
                        ProtobufResolutionIssue.FieldLocation.Reservation("536870912"),
                        536_870_912,
                    )
                )
        }

        "enum reservations accept a single number in the implementation range" {
            resolveEnum(ReservedIndex(19_500)).shouldBeTypeOf<DomainResolution.Success<ResolvedProtobufSchemaModel>>()
        }
    })
