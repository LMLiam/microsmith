package me.liam.microsmith.dsl.schemas.protobuf.reserved

import me.liam.microsmith.dsl.schemas.protobuf.support.IndexAllocator
import me.liam.microsmith.dsl.schemas.protobuf.support.NameRegistry

internal fun buildReservedDeclarations(allocator: IndexAllocator, nameRegistry: NameRegistry): List<Reserved> =
    buildList {
        allocator.reserved().sortedBy(IntRange::first).mapTo(this, Reserved::fromRange)
        nameRegistry.reserved().sorted().mapTo(this, ::ReservedName)
    }
