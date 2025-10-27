package me.liam.microsmith.dsl.schemas.protobuf.extensions

internal fun MutableSet<IntRange>.merge(newRange: IntRange) {
    val merged = mutableSetOf<IntRange>()
    var current = newRange
    for (range in sortedBy { it.first }) {
        if (range.last + 1 < current.first || current.last + 1 < range.first) {
            merged += range
        } else {
            current = minOf(range.first, current.first)..maxOf(range.last, current.last)
        }
    }
    merged += current
    clear()
    addAll(merged)
}

internal fun MutableSet<IntRange>.merge(newRanges: Set<IntRange>) {
    newRanges.forEach { this.merge(it) }
}