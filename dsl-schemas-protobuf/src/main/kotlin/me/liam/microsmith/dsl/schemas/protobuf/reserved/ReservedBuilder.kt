package me.liam.microsmith.dsl.schemas.protobuf.reserved

import me.liam.microsmith.dsl.schemas.protobuf.ReservedScope
import me.liam.microsmith.dsl.schemas.protobuf.support.IndexAllocator
import me.liam.microsmith.dsl.schemas.protobuf.support.NameRegistry

class ReservedBuilder(
    private val allocator: IndexAllocator,
    private val names: NameRegistry
) : ReservedScope {
    override fun index(index: Int) {
        allocator.reserve(index)
    }

    override fun name(name: String) {
        names.reserve(name)
    }

    override fun range(range: IntRange) {
        allocator.reserve(range)
    }

    override fun range(range: MaxRange) {
        allocator.reserve(range.from..max.VALUE)
    }
}