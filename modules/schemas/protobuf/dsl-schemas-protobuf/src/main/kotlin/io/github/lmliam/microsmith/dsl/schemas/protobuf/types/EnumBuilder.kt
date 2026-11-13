package io.github.lmliam.microsmith.dsl.schemas.protobuf.types

import io.github.lmliam.microsmith.dsl.schemas.protobuf.internal.allocation.IndexAllocator
import io.github.lmliam.microsmith.dsl.schemas.protobuf.internal.names.NameRegistry
import io.github.lmliam.microsmith.dsl.schemas.protobuf.numbers.ProtobufNumberLimits
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.MaxRange
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedBuilder
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.buildReservedDeclarations
import io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.enum.EnumScope
import io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.enum.EnumValueScope
import io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.reserved.ReservedScope

internal class EnumBuilder(private val name: String) : EnumScope {
    private val allocator =
        IndexAllocator(min = ProtobufNumberLimits.ENUM_MIN, max = ProtobufNumberLimits.ENUM_MAX, firstAutomatic = 1)
    private val nameRegistry = NameRegistry()

    private val values = mutableSetOf<EnumValue>()

    override fun value(name: String, block: EnumValueScope.() -> Unit) {
        nameRegistry.validate(name)

        val index = allocator.allocate(EnumValueBuilder().apply(block).index)

        nameRegistry.use(name)
        values += EnumValue(name, index)
    }

    override fun reserved(vararg indexes: Int) = indexes.forEach { allocator.reserve(it..it) }

    override fun reserved(vararg indexRanges: IntRange) = indexRanges.forEach { allocator.reserve(it) }

    override fun reserved(toMax: MaxRange) = allocator.reserveToMax(toMax.from)

    override fun reserved(vararg names: String) = names.forEach { this.nameRegistry.reserve(it) }

    override fun reserved(block: ReservedScope.() -> Unit) {
        ReservedBuilder(allocator, nameRegistry).apply(block)
    }

    fun build() =
        Enum(
            name = name,
            values =
                (values + defaultZeroValue()).sortedWith(compareBy<EnumValue> { it.index != 0 }.thenBy { it.index }),
            reserved = buildReservedDeclarations(allocator, nameRegistry),
        )

    private fun defaultZeroValue(): List<EnumValue> {
        if (values.any { it.index == 0 } || allocator.reserved().any { 0 in it }) return emptyList()

        val defaultName = unspecifiedValueName(name)
        nameRegistry.validate(defaultName, "the default zero value of enum $name")
        return listOf(EnumValue(defaultName, 0))
    }
}
