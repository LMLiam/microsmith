package me.liam.microsmith.dsl.schemas.protobuf.support

import me.liam.microsmith.dsl.schemas.protobuf.extensions.merge
import me.liam.microsmith.dsl.schemas.protobuf.reserved.Max

class IndexAllocator(
    private val min: Int,
    private val protoReserved: IntRange? = null
) {
    private val reserved = mutableSetOf<IntRange>()
    fun reserved() = reserved.toSet()

    private val used = mutableSetOf<Int>()
    private var next = min

    fun allocate(requested: Int? = null): Int =
        if (requested != null) {
            validate(requested)
            used += requested
            requested
        } else {
            val candidate = generateSequence(next) { it + 1 }
                .first { c ->
                    c !in used &&
                            reserved.none { c in it } &&
                            protoReserved?.contains(c) != true
                }
            validate(candidate)
            used += candidate
            next = candidate + 1
            candidate
        }

    fun reserve(index: Int) {
        validate(index)
        reserved.merge(index..index)
    }

    fun reserve(range: IntRange) {
        validate(range)
        reserved.merge(range)
    }

    fun validate(index: Int) {
        require(index in min..Max.VALUE) { "Invalid index: $index" }
        protoReserved?.let {
            require(index !in it) { "Index $index is in proto reserved range" }
        }
        require(index !in used) { "Index $index already used" }
    }

    fun validate(range: IntRange) {
        validate(range.first)
        validate(range.last)
        require(used.none { it in range }) { "Range $range overlaps with used indexes" }
        require(reserved.none { existing ->
            existing.first <= range.last && range.first <= existing.last
        }) { "Range $range overlaps with already reserved ranges" }
    }
}