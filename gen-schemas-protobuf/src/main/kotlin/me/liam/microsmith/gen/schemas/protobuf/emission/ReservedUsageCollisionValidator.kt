package me.liam.microsmith.gen.schemas.protobuf.emission

import me.liam.microsmith.dsl.schemas.protobuf.reserved.Reserved
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedIndex
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedName
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedRange
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedToMax
import me.liam.microsmith.dsl.schemas.protobuf.types.Enum
import me.liam.microsmith.dsl.schemas.protobuf.types.Message

/**
 * Reapplies builder-level reserved-name and reserved-number invariants for programmatic schemas.
 */
internal object ReservedUsageCollisionValidator {
    fun validate(message: Message) {
        validateNames(
            ownerType = "Message",
            ownerName = message.name,
            reservedNames = message.reserved.filterIsInstance<ReservedName>().map(ReservedName::name),
            usedNames = buildList {
                message.fields.forEach { add(it.name) }
                message.oneofs.forEach { oneof -> oneof.fields.forEach { add(it.name) } }
            },
        )
        validateNumbers(
            ownerType = "Message",
            ownerName = message.name,
            reserved = message.reserved,
            usedNumbers = buildList {
                message.fields.forEach { add(it.index) }
                message.oneofs.forEach { oneof -> oneof.fields.forEach { add(it.index) } }
            },
        )
    }

    fun validate(enum: Enum) {
        validateNames(
            ownerType = "Enum",
            ownerName = enum.name,
            reservedNames = enum.reserved.filterIsInstance<ReservedName>().map(ReservedName::name),
            usedNames = enum.values.map { it.name },
        )
        validateNumbers(
            ownerType = "Enum",
            ownerName = enum.name,
            reserved = enum.reserved,
            usedNumbers = enum.values.map { it.index },
        )
    }

    private fun validateNames(
        ownerType: String,
        ownerName: String,
        reservedNames: List<String>,
        usedNames: List<String>,
    ) {
        val duplicateReservedNames = reservedNames.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        require(duplicateReservedNames.isEmpty()) {
            "$ownerType '$ownerName' has duplicate reserved names: ${duplicateReservedNames.sorted().joinToString(
                ", ",
            )}."
        }

        val reservedNameSet = reservedNames.toSet()
        val collisions = usedNames.distinct().filter { it in reservedNameSet }
        require(collisions.isEmpty()) {
            "$ownerType '$ownerName' uses reserved names: ${collisions.sorted().joinToString(", ")}."
        }
    }

    private fun validateNumbers(
        ownerType: String,
        ownerName: String,
        reserved: List<Reserved>,
        usedNumbers: List<Int>,
    ) {
        val reservedRanges = reserved.filterNot { it is ReservedName }.map(::toReservedSpan)
        requireNoReservedRangeOverlaps(ownerType, ownerName, reservedRanges)

        val collisions = usedNumbers.distinct().filter { number -> reservedRanges.any { number in it.range } }
        require(collisions.isEmpty()) {
            "$ownerType '$ownerName' uses reserved numbers: ${collisions.sorted().joinToString(", ")}."
        }
    }

    private fun requireNoReservedRangeOverlaps(
        ownerType: String,
        ownerName: String,
        reservedRanges: List<ReservedSpan>,
    ) {
        val overlaps =
            reservedRanges
                .sortedBy { it.range.first }
                .zipWithNext()
                .filter { (left, right) -> left.range.last >= right.range.first }
                .map { (left, right) -> "${left.description} overlaps ${right.description}" }

        require(overlaps.isEmpty()) {
            "$ownerType '$ownerName' has overlapping reserved ranges: ${overlaps.joinToString("; ")}."
        }
    }

    private fun toReservedSpan(reserved: Reserved): ReservedSpan = when (reserved) {
        is ReservedIndex -> ReservedSpan(
            description = reserved.index.toString(),
            range = reserved.index..reserved.index,
        )
        is ReservedRange -> ReservedSpan(
            description = "${reserved.indexRange.first} to ${reserved.indexRange.last}",
            range = reserved.indexRange,
        )
        is ReservedToMax -> ReservedSpan(
            description = "${reserved.from} to max",
            range = reserved.from..ProtobufFieldNumbers.MAX_FIELD_NUMBER,
        )
        is ReservedName -> error("Reserved names do not produce numeric spans.")
    }
}

private data class ReservedSpan(
    val description: String,
    val range: IntRange,
)
