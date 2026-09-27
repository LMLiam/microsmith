package io.github.lmliam.microsmith.dsl.schemas.protobuf.types

import io.github.lmliam.microsmith.dsl.schemas.protobuf.internal.allocation.IndexAllocator
import io.github.lmliam.microsmith.dsl.schemas.protobuf.internal.names.NameRegistry
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.Max
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.MaxRange
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedBuilder
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.buildReservedDeclarations
import io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.enum.EnumScope
import io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.enum.EnumValueScope
import io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.reserved.ReservedScope

internal class EnumBuilder(private val name: String) : EnumScope {
    private val allocator = IndexAllocator(0)
    private val nameRegistry = NameRegistry()

    private val values = mutableSetOf<EnumValue>()

    init {
        value(Enum.UNSPECIFIED) {
            index(0)
        }
    }

    override fun value(name: String, block: EnumValueScope.() -> Unit) {
        nameRegistry.use(name)

        EnumValueBuilder()
            .apply(block)
            .let { allocator.allocate(it.index) }
            .let { EnumValue(name, it) }
            .also { values += it }
    }

    override fun reserved(vararg indexes: Int) = indexes.forEach { allocator.reserve(it..it) }

    override fun reserved(vararg indexRanges: IntRange) = indexRanges.forEach { allocator.reserve(it) }

    override fun reserved(toMax: MaxRange) = allocator.reserve(toMax.from..Max.VALUE)

    override fun reserved(vararg names: String) = names.forEach { this.nameRegistry.reserve(it) }

    override fun reserved(block: ReservedScope.() -> Unit) {
        ReservedBuilder(allocator, nameRegistry).apply(block)
    }

    fun build() = Enum(
        name = name,
        values = values.sortedBy { it.index },
        reserved = buildReservedDeclarations(allocator, nameRegistry),
    )
}
