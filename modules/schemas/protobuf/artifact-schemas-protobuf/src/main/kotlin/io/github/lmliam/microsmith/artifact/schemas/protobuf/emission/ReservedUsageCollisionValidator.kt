package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission

import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnum
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufMessage
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.ReservedSpan

internal fun validateReservedUsage(message: ResolvedProtobufMessage) {
    validateNames(
        owner = ReservedUsageOwner.message(message.name.value),
        reservedNames = message.reservations.names,
        usedNames =
            buildList {
                message.fields.forEach { add(it.name.value) }
                message.oneofs.forEach { oneof -> oneof.fields.forEach { add(it.name.value) } }
            },
    )
    validateNumbers(
        owner = ReservedUsageOwner.message(message.name.value),
        reserved = message.reservations.spans,
        usedNumbers =
            buildList {
                message.fields.forEach { add(it.number.value) }
                message.oneofs.forEach { oneof -> oneof.fields.forEach { add(it.number.value) } }
            },
    )
}

internal fun validateReservedUsage(enum: ResolvedProtobufEnum) {
    validateNames(
        owner = ReservedUsageOwner.enum(enum.name.value),
        reservedNames = enum.reservations.names,
        usedNames = enum.values.map { it.name.value },
    )
    validateNumbers(
        owner = ReservedUsageOwner.enum(enum.name.value),
        reserved = enum.reservations.spans,
        usedNumbers = enum.values.map { it.number.value },
    )
}

private fun validateNames(owner: ReservedUsageOwner, reservedNames: List<ProtoIdentifier>, usedNames: List<String>) {
    val duplicateReservedNames = reservedNames.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
    require(duplicateReservedNames.isEmpty()) {
        val duplicateNames = duplicateReservedNames.map { it.value }.sorted().joinToString(", ")
        "${owner.displayName} '${owner.name}' has duplicate reserved names: $duplicateNames"
    }

    val reservedNameSet = reservedNames.map { it.value }.toSet()
    val collisions = usedNames.distinct().filter { it in reservedNameSet }
    require(collisions.isEmpty()) {
        "${owner.displayName} '${owner.name}' uses reserved names: ${collisions.sorted().joinToString(", ")}"
    }
}

private fun validateNumbers(owner: ReservedUsageOwner, reserved: List<ReservedSpan>, usedNumbers: List<Int>) {
    requireNoReservedRangeOverlaps(owner, reserved)

    val collisions = usedNumbers.distinct().filter { number -> reserved.any { number in it } }
    require(collisions.isEmpty()) {
        "${owner.displayName} '${owner.name}' uses reserved numbers: ${collisions.sorted().joinToString(", ")}"
    }
}

private fun requireNoReservedRangeOverlaps(owner: ReservedUsageOwner, reserved: List<ReservedSpan>) {
    val overlaps =
        reserved
            .sortedBy { it.first }
            .zipWithNext()
            .filter { (left, right) -> left.last >= right.first }
            .map { (left, right) -> "${left.notation} overlaps ${right.notation}" }

    require(overlaps.isEmpty()) {
        "${owner.displayName} '${owner.name}' has overlapping reserved ranges: ${overlaps.joinToString("; ")}"
    }
}
