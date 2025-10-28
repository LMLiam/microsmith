package me.liam.microsmith.dsl.schemas.protobuf.extensions

internal fun MutableSet<IntRange>.merge(newRange: IntRange) {
    val merged = sortedBy { it.first }
        .fold(newRange) { current, range ->
            if (range.last + 1 < current.first || current.last + 1 < range.first) {
                // disjoint, keep as is
                add(range)
                current
            } else {
                // overlapping or adjacent, merge into a single range
                minOf(range.first, current.first)..maxOf(range.last, current.last)
            }
        }

    clear()
    addAll(filterNot { it overlaps merged })
    add(merged)
}

private infix fun IntRange.overlaps(other: IntRange): Boolean =
    !(last + 1 < other.first || other.last + 1 < first)

internal fun MutableSet<IntRange>.merge(newRanges: Iterable<IntRange>) =
    newRanges.forEach { merge(it) }