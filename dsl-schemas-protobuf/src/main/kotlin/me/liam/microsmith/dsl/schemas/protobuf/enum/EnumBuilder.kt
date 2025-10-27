package me.liam.microsmith.dsl.schemas.protobuf.enum

import me.liam.microsmith.dsl.schemas.protobuf.EnumScope

class EnumBuilder(private val name: String) : EnumScope {
    private val values = mutableSetOf<EnumValue>()
    private var nextIndex = 1

    init {
        values += EnumValue("UNSPECIFIED", 0)
    }

    override fun value(name: String) {
        require(name.isNotBlank()) { "Enum value name cannot be blank." }
        require(values.none { it.name == name }) { "Duplicate enum value: $name" }

        values += EnumValue(name, nextIndex++)
    }

    fun build() = Enum(name, values.sortedBy { it.index })
}