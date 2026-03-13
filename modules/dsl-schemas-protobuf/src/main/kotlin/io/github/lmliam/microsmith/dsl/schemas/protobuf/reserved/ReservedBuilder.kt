package io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved

import io.github.lmliam.microsmith.dsl.schemas.protobuf.ReservedScope
import io.github.lmliam.microsmith.dsl.schemas.protobuf.support.IndexAllocator
import io.github.lmliam.microsmith.dsl.schemas.protobuf.support.NameRegistry

internal class ReservedBuilder(private val allocator: IndexAllocator, private val names: NameRegistry) : ReservedScope {
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
