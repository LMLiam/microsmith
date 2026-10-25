package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation

import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedIndex
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedName
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedRange
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedToMax
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class ProtobufReservationValidationTests :
    StringSpec({
        "message reservations reject a name that the message declares" {
            val issues =
                validateMessageReservations(
                    schemaName = "User",
                    reservations = listOf(ReservedName("id")),
                    usedNames = listOf("id", "email"),
                    usedNumbers = emptyList(),
                )

            issues.shouldContainExactly(ProtobufResolutionIssue.ReservedNameCollision("User", listOf("id")))
        }

        "message reservations reject a name that is reserved twice" {
            val issues =
                validateMessageReservations(
                    schemaName = "User",
                    reservations = listOf(ReservedName("email"), ReservedName("email")),
                    usedNames = emptyList(),
                    usedNumbers = emptyList(),
                )

            issues.shouldContainExactly(ProtobufResolutionIssue.DuplicateReservedNames("User", listOf("email")))
        }

        "message reservations reject a number that the message declares" {
            val issues =
                validateMessageReservations(
                    schemaName = "User",
                    reservations = listOf(ReservedIndex(5)),
                    usedNames = emptyList(),
                    usedNumbers = listOf(5, 6),
                )

            issues.shouldContainExactly(ProtobufResolutionIssue.ReservedNumberCollision("User", listOf(5)))
        }

        "message reservations reject overlapping ranges" {
            val issues =
                validateMessageReservations(
                    schemaName = "User",
                    reservations = listOf(ReservedRange(10..20), ReservedRange(15..30)),
                    usedNames = emptyList(),
                    usedNumbers = emptyList(),
                )

            issues.shouldContainExactly(ProtobufResolutionIssue.OverlappingReservedNumbers("User"))
        }

        "message reservations reject a descending range" {
            val issues =
                validateMessageReservations(
                    schemaName = "User",
                    reservations = listOf(ReservedRange(IntRange(20, 10))),
                    usedNames = emptyList(),
                    usedNumbers = emptyList(),
                )

            issues.shouldContainExactly(ProtobufResolutionIssue.InvalidReservationRange("User", 20, 10))
        }

        "message reservations reject a number outside the protobuf field range" {
            val issues =
                validateMessageReservations(
                    schemaName = "User",
                    reservations = listOf(ReservedIndex(0)),
                    usedNames = emptyList(),
                    usedNumbers = emptyList(),
                )

            issues.shouldContainExactly(
                ProtobufResolutionIssue.InvalidFieldNumber(
                    "User",
                    ProtobufResolutionIssue.FieldLocation.Reservation("0"),
                    0,
                )
            )
        }

        "message reservations name the range in a field number issue" {
            val issues =
                validateMessageReservations(
                    schemaName = "User",
                    reservations = listOf(ReservedRange(0..10)),
                    usedNames = emptyList(),
                    usedNumbers = emptyList(),
                )

            issues
                .single()
                .shouldBe(
                    ProtobufResolutionIssue.InvalidFieldNumber(
                        "User",
                        ProtobufResolutionIssue.FieldLocation.Reservation("0..10"),
                        0,
                    )
                )
        }

        "message reservations report name issues before number issues" {
            val issues =
                validateMessageReservations(
                    schemaName = "User",
                    reservations = listOf(ReservedName("id"), ReservedIndex(5)),
                    usedNames = listOf("id"),
                    usedNumbers = listOf(5),
                )

            issues.map { issue -> issue::class } shouldContainExactly
                listOf(
                    ProtobufResolutionIssue.ReservedNameCollision::class,
                    ProtobufResolutionIssue.ReservedNumberCollision::class,
                )
        }

        "enum reservations accept a range that reaches the maximum" {
            val issues =
                validateEnumReservations(
                    schemaName = "Status",
                    reservations = listOf(ReservedToMax(100)),
                    usedNames = emptyList(),
                    usedNumbers = emptyList(),
                )

            issues.shouldBeEmpty()
        }

        "enum reservations accept negative values" {
            val issues =
                validateEnumReservations(
                    schemaName = "Status",
                    reservations = listOf(ReservedIndex(-5)),
                    usedNames = emptyList(),
                    usedNumbers = emptyList(),
                )

            issues.shouldBeEmpty()
        }

        "enum reservations reject a descending range" {
            val issues =
                validateEnumReservations(
                    schemaName = "Status",
                    reservations = listOf(ReservedRange(IntRange(20, 10))),
                    usedNames = emptyList(),
                    usedNumbers = emptyList(),
                )

            issues.shouldContainExactly(ProtobufResolutionIssue.InvalidReservationRange("Status", 20, 10))
        }
    })
