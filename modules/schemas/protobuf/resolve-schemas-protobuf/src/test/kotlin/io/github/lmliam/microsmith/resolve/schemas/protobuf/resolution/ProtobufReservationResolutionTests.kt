package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import io.github.lmliam.microsmith.dsl.schemas.SchemasExtension
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ScalarField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.Reserved
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedIndex
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedName
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedRange
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedToMax
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Enum
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.EnumValue
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnum
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufMessage
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufReservations
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchemaModel
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.ReservedSpan
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.types.shouldBeTypeOf

class ProtobufReservationResolutionTests :
    StringSpec({
        val resolver = ProtobufSchemasResolver()

        fun resolveMessage(vararg reservations: Reserved, fields: List<Pair<String, Int>> = emptyList()) =
            resolver.resolve(
                SchemasExtension(
                    setOf(
                        ProtobufSchema(
                            "User",
                            Message(
                                "User",
                                fields.map { (name, number) -> ScalarField(name, number, PrimitiveType.STRING) },
                                reserved = reservations.toList(),
                            ),
                        )
                    )
                )
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

        fun DomainResolution<*>.issues() = shouldBeTypeOf<DomainResolution.Failure>().issues.toList()

        fun DomainResolution<*>.declaration() =
            shouldBeTypeOf<DomainResolution.Success<ResolvedProtobufSchemaModel>>().model.schemas.single().declaration

        fun DomainResolution<*>.messageReservations() =
            declaration().shouldBeTypeOf<ResolvedProtobufMessage>().reservations

        fun DomainResolution<*>.enumReservations() = declaration().shouldBeTypeOf<ResolvedProtobufEnum>().reservations

        fun ResolvedProtobufReservations.notations() = spans.map(ReservedSpan::notation)

        "message reservations accept a single number in the implementation range" {
            resolveMessage(ReservedIndex(19_500)).messageReservations().notations() shouldContainExactly listOf("19500")
        }

        "message reservations accept a range that straddles the implementation range" {
            resolveMessage(ReservedRange(18_000..20_000)).messageReservations().notations() shouldContainExactly
                listOf("18000 to 20000")
        }

        "message reservations accept a range that is exactly the implementation range" {
            resolveMessage(ReservedRange(19_000..19_999)).messageReservations().notations() shouldContainExactly
                listOf("19000 to 19999")
        }

        "message reservations accept a range to max that starts in the implementation range" {
            resolveMessage(ReservedToMax(19_500)).messageReservations().notations() shouldContainExactly
                listOf("19500 to max")
        }

        "message reservations accept a range to max" {
            resolveMessage(ReservedToMax(1_000)).messageReservations().notations() shouldContainExactly
                listOf("1000 to max")
        }

        "message reservations reject zero" {
            resolveMessage(ReservedIndex(0)).issues() shouldContainExactly
                listOf(
                    ProtobufResolutionIssue.InvalidFieldNumber(
                        "User",
                        ProtobufResolutionIssue.FieldLocation.Reservation("0"),
                        0,
                    )
                )
        }

        "message reservations reject a number above the maximum field number" {
            resolveMessage(ReservedIndex(536_870_912)).issues() shouldContainExactly
                listOf(
                    ProtobufResolutionIssue.InvalidFieldNumber(
                        "User",
                        ProtobufResolutionIssue.FieldLocation.Reservation("536870912"),
                        536_870_912,
                    )
                )
        }

        "message reservations resolve names and numbers in the form they were written" {
            val reservations =
                resolveMessage(
                        ReservedName("old"),
                        ReservedIndex(5),
                        ReservedRange(10..20),
                        ReservedToMax(100),
                    )
                    .messageReservations()

            reservations.names.map { it.value } shouldContainExactly listOf("old")
            reservations.notations() shouldContainExactly listOf("5", "10 to 20", "100 to max")
        }

        "message reservations still reject a number inside a range that has an invalid endpoint" {
            resolveMessage(ReservedRange(0..10), fields = listOf("a" to 5)).issues() shouldContainExactly
                listOf(
                    ProtobufResolutionIssue.InvalidFieldNumber(
                        "User",
                        ProtobufResolutionIssue.FieldLocation.Reservation("0..10"),
                        0,
                    ),
                    ProtobufResolutionIssue.ReservedNumberCollision("User", listOf(5)),
                )
        }

        "message reservations reject a name that the message declares" {
            resolveMessage(ReservedName("id"), fields = listOf("id" to 1, "email" to 2)).issues() shouldContainExactly
                listOf(ProtobufResolutionIssue.ReservedNameCollision("User", listOf("id")))
        }

        "message reservations reject a name that is reserved twice" {
            resolveMessage(ReservedName("email"), ReservedName("email")).issues() shouldContainExactly
                listOf(ProtobufResolutionIssue.DuplicateReservedNames("User", listOf("email")))
        }

        "message reservations reject a name that is not an identifier" {
            resolveMessage(ReservedName("1bad")).issues() shouldContainExactly
                listOf(
                    ProtobufResolutionIssue.InvalidIdentifier(
                        "User",
                        ProtobufResolutionIssue.IdentifierLocation.Reservation("1bad"),
                        "1bad",
                    )
                )
        }

        "message reservations reject a number that the message declares" {
            resolveMessage(ReservedIndex(5), fields = listOf("a" to 5, "b" to 6)).issues() shouldContainExactly
                listOf(ProtobufResolutionIssue.ReservedNumberCollision("User", listOf(5)))
        }

        "message reservations reject overlapping ranges" {
            resolveMessage(ReservedRange(10..20), ReservedRange(15..30)).issues() shouldContainExactly
                listOf(ProtobufResolutionIssue.OverlappingReservedNumbers("User"))
        }

        "message reservations reject a descending range" {
            resolveMessage(ReservedRange(IntRange(20, 10))).issues() shouldContainExactly
                listOf(ProtobufResolutionIssue.InvalidReservationRange("User", 20, 10))
        }

        "message reservations name the range in a field number issue" {
            resolveMessage(ReservedRange(0..10)).issues() shouldContainExactly
                listOf(
                    ProtobufResolutionIssue.InvalidFieldNumber(
                        "User",
                        ProtobufResolutionIssue.FieldLocation.Reservation("0..10"),
                        0,
                    )
                )
        }

        "message reservations report name issues before number issues" {
            val issues = resolveMessage(ReservedName("id"), ReservedIndex(5), fields = listOf("id" to 5)).issues()

            issues.map { issue -> issue::class } shouldContainExactly
                listOf(
                    ProtobufResolutionIssue.ReservedNameCollision::class,
                    ProtobufResolutionIssue.ReservedNumberCollision::class,
                )
        }

        "enum reservations accept a range that reaches the maximum" {
            resolveEnum(ReservedToMax(100)).enumReservations().notations() shouldContainExactly listOf("100 to max")
        }

        "enum reservations accept negative values" {
            resolveEnum(ReservedIndex(-5)).enumReservations().notations() shouldContainExactly listOf("-5")
        }

        "enum reservations reject a descending range" {
            resolveEnum(ReservedRange(IntRange(20, 10))).issues() shouldContainExactly
                listOf(ProtobufResolutionIssue.InvalidReservationRange("Status", 20, 10))
        }

        "enum reservations accept a single number in the implementation range" {
            resolveEnum(ReservedIndex(19_500)).enumReservations().notations() shouldContainExactly listOf("19500")
        }
    })
