package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation

import arrow.core.raise.Raise
import arrow.core.raise.context.ensure
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufRejection

context(_: Raise<ResolvedProtobufRejection>)
internal fun checkUniqueReservedNames(names: List<String>) =
    checkNoDuplicates(names, ResolvedProtobufRejection::DuplicateReservedNames)

context(_: Raise<ResolvedProtobufRejection>)
internal fun checkNoReservedNameUse(reservedNames: List<String>, usedNames: List<String>) {
    val collisions = reservedNames.toSet().intersect(usedNames.toSet()).sorted()

    ensure(collisions.isEmpty()) { ResolvedProtobufRejection.ReservedNameCollision(collisions) }
}

context(_: Raise<ResolvedProtobufRejection>)
internal fun checkNoOverlap(occupied: List<IntRange>) {
    val overlapping = occupied.sortedBy(IntRange::first).zipWithNext().any { (left, right) -> left.last >= right.first }

    ensure(!overlapping) { ResolvedProtobufRejection.OverlappingReservedNumbers }
}

context(_: Raise<ResolvedProtobufRejection>)
internal fun checkNoReservedNumberUse(occupied: List<IntRange>, usedNumbers: List<Int>) {
    val collisions = usedNumbers.distinct().filter { number -> occupied.any { number in it } }.sorted()

    ensure(collisions.isEmpty()) { ResolvedProtobufRejection.ReservedNumberCollision(collisions) }
}
