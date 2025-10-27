package me.liam.microsmith.dsl.schemas.protobuf.enum

import me.liam.microsmith.dsl.schemas.protobuf.EnumScope
import me.liam.microsmith.dsl.schemas.protobuf.EnumValueScope
import me.liam.microsmith.dsl.schemas.protobuf.ReservedScope
import me.liam.microsmith.dsl.schemas.protobuf.reserved.*
import me.liam.microsmith.dsl.schemas.protobuf.support.IndexAllocator
import me.liam.microsmith.dsl.schemas.protobuf.support.NameRegistry

class EnumBuilder(private val name: String) : EnumScope {
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
        val builder = EnumValueBuilder().apply(block)
        val allocated = allocator.allocate(builder.index)
        values += EnumValue(name, allocated)
    }

    override fun reserved(vararg indexes: Int) =
        indexes.forEach { allocator.reserve(it..it) }

    override fun reserved(vararg indexRanges: IntRange) =
        indexRanges.forEach { allocator.reserve(it) }

    override fun reserved(toMax: MaxRange) =
        allocator.reserve(toMax.from..Max.VALUE)

    override fun reserved(vararg names: String) =
        names.forEach { this.nameRegistry.reserve(it) }

    override fun reserved(block: ReservedScope.() -> Unit) {
        ReservedBuilder(allocator, nameRegistry).apply(block)
    }

    fun build() = Enum(
        name,
        values.sortedBy { it.index },
        allocator.reserved()
            .sortedBy { it.first }
            .map(Reserved::fromRange) +
                nameRegistry.reserved().sorted().map(::ReservedName)
    )
}