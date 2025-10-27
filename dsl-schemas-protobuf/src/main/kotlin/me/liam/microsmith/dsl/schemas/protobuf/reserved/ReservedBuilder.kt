package me.liam.microsmith.dsl.schemas.protobuf.reserved

import me.liam.microsmith.dsl.schemas.protobuf.ReservedScope
import me.liam.microsmith.dsl.schemas.protobuf.extensions.merge

class ReservedBuilder(
    internal val reservedIndexes: MutableSet<IntRange> = mutableSetOf(),
    internal val reservedNames: MutableSet<String> = mutableSetOf(),
) : ReservedScope {
    override fun index(index: Int) {
        reservedIndexes.merge(index..index)
    }

    override fun name(name: String) {
        reservedNames += name
    }

    override fun range(range: IntRange) {
        reservedIndexes.merge(range)
    }

    override fun range(range: MaxRange) {
        reservedIndexes.merge(range.from..max.VALUE)
    }
}