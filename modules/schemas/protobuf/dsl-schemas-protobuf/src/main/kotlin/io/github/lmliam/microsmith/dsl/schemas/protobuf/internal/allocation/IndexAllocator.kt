package io.github.lmliam.microsmith.dsl.schemas.protobuf.internal.allocation

import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.merge

internal class IndexAllocator(
    private val min: Int,
    val max: Int,
    private val protoReserved: IntRange? = null,
    firstAutomatic: Int = min,
) {
    private val reserved = mutableSetOf<IntRange>()

    fun reserved() = reserved.toSet()

    private val used = mutableSetOf<Int>()
    private var next = firstAutomatic.toLong()

    fun allocate(requested: Int? = null): Int {
        if (requested != null) {
            validate(requested)
            used += requested
            return requested
        }

        return nextFreeIndex().also { index ->
            used += index
            next = index.toLong() + 1
        }
    }

    private fun nextFreeIndex(): Int {
        val blockedRanges = reserved + listOfNotNull(protoReserved)
        var candidate = next
        while (candidate <= max) {
            val index = candidate.toInt()
            val blocking = blockedRanges.firstOrNull { index in it }
            candidate =
                when {
                    blocking != null -> blocking.last.toLong() + 1
                    index in used -> candidate + 1
                    else -> return index
                }
        }
        throw IllegalArgumentException("No index available up to $max")
    }

    fun reserveToMax(from: Int) = reserve(from..max)

    fun reserve(index: Int) {
        validateReservable(index)
        reserved.merge(index..index)
    }

    fun reserve(range: IntRange) {
        validateReservable(range)
        reserved.merge(range)
    }

    fun validate(index: Int) {
        validateReservable(index)
        protoReserved?.let { require(index !in it) { "Index $index is in proto reserved range" } }
    }

    private fun validateReservable(index: Int) {
        require(index in min..max) { "Invalid index: $index" }
        require(index !in used) { "Index $index already used" }
        require(reserved.none { index in it }) { "Index $index is in reserved range" }
    }

    private fun validateReservable(range: IntRange) {
        validateReservable(range.first)
        validateReservable(range.last)
        validateNoOverlap(range)
    }

    private fun validateNoOverlap(range: IntRange) {
        require(used.none { it in range }) { "Range $range overlaps with used indexes" }
        require(reserved.none { existing -> existing.first <= range.last && range.first <= existing.last }) {
            "Range $range overlaps with already reserved ranges"
        }
    }
}
