package me.liam.microsmith.dsl.schemas.protobuf.enum

import me.liam.microsmith.dsl.schemas.protobuf.EnumScope
import me.liam.microsmith.dsl.schemas.protobuf.ReservedScope
import me.liam.microsmith.dsl.schemas.protobuf.extensions.merge
import me.liam.microsmith.dsl.schemas.protobuf.reserved.Max
import me.liam.microsmith.dsl.schemas.protobuf.reserved.MaxRange
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedBuilder
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedIndex
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedName
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedRange
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedToMax

class EnumBuilder(private val name: String) : EnumScope {
    private val values = mutableSetOf<EnumValue>()
    private val reservedIndexes = mutableSetOf<IntRange>()
    private val reservedNames = mutableSetOf<String>()
    private var nextIndex = 1

    init {
        values += EnumValue(Enum.UNSPECIFIED, 0)
    }

    override fun value(name: String) {
        validate(name)
        values += EnumValue(name, allocateIndex())
    }

    override fun reserved(vararg indexes: Int) {
        indexes.forEach { reserve(it..it) }
    }

    override fun reserved(vararg indexRanges: IntRange) {
        indexRanges.forEach { reserve(it) }
    }

    override fun reserved(toMax: MaxRange) {
        reserve(toMax.from..Max.VALUE)
    }

    override fun reserved(vararg names: String) {
        names.forEach { reserve(it) }
    }

    override fun reserved(block: ReservedScope.() -> Unit) {
        val builder = ReservedBuilder().apply(block)
        builder.reservedIndexes.forEach { reserve(it) }
        builder.reservedNames.forEach { reserve(it) }
    }

    fun build() = Enum(
        name,
        values.sortedBy { it.index },
        reservedIndexes.sortedBy { it.first }
            .map { r ->
                when {
                    r.first == r.last -> ReservedIndex(r.first)
                    r.last == Max.VALUE -> ReservedToMax(r.first)
                    else -> ReservedRange(r)
                }
            } + reservedNames.sorted().map { ReservedName(it) }
    )

    private fun allocateIndex(idx: Int? = null): Int =
        if (idx != null) {
            validate(idx)
            values.none { it.index == idx } || error("Enum index $idx already used")
            idx
        } else {
            val candidate = generateSequence(nextIndex) { it + 1 }
                .first { c ->
                    reservedIndexes.none { c in it } &&
                            values.none { it.index == c }
                }
            validate(candidate)
            nextIndex = candidate + 1
            candidate
        }

    private fun validate(name: String) {
        require(name.isNotBlank()) { "Enum name cannot be blank." }
        require(values.none { it.name == name }) { "Duplicate enum value: $name" }
        require(name !in reservedNames) { "Enum name already reserved: $name" }
    }

    private fun validate(index: Int) {
        require(index in 0..Max.VALUE) { "Invalid enum index: $index" }
        require(reservedIndexes.none { index in it }) { "Enum index $index is reserved." }
        require(index !in values.map { it.index }) { "Enum index $index already used." }
    }

    private fun validate(range: IntRange) {
        validate(range.first)
        validate(range.last)
    }

    private fun reserve(range: IntRange) {
        validate(range)
        reservedIndexes.merge(range)
    }

    private fun reserve(name: String) {
        validate(name)
        reservedNames += name
    }
}