package io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved

import io.github.lmliam.microsmith.dsl.schemas.protobuf.support.IndexAllocator
import io.github.lmliam.microsmith.dsl.schemas.protobuf.support.NameRegistry

internal fun buildReservedDeclarations(allocator: IndexAllocator, nameRegistry: NameRegistry): List<Reserved> =
    buildList {
        allocator.reserved().sortedBy(IntRange::first).mapTo(this, Reserved::fromRange)
        nameRegistry.reserved().sorted().mapTo(this, ::ReservedName)
    }
