package me.liam.microsmith.dsl.schemas.protobuf

class MessageBuilder(private val name: String) : MessageScope {
    private val fields = mutableMapOf<String, Field>()
    private val usedIndexes = mutableSetOf<Int>()
    private var fieldIndex = 1

    override fun optional(field: Field) {
        fields[field.name] = field.copy(optional = true)
    }

    override fun int32(name: String, block: FieldScope.() -> Unit): Field {
        return addField(name, FieldType.INT32, block)
    }

    override fun string(name: String, block: FieldScope.() -> Unit): Field {
        return addField(name, FieldType.STRING, block)
    }

    private fun addField(name: String, type: FieldType, block: FieldScope.() -> Unit): Field {
        require(name.isNotBlank()) { "Field name cannot be blank" }
        require(!fields.containsKey(name)) { "Duplicate field name: $name" }

        val builder = FieldBuilder(fieldIndex, false).apply(block)

        validateIndex(builder.index)

        require(!usedIndexes.contains(builder.index)) { "Duplicate field number: ${builder.index}" }

        val field = Field(name, type, builder.index, builder.optional)
        fields[name] = field
        usedIndexes += builder.index

        fieldIndex = maxOf(fieldIndex + 1, builder.index + 1)
        return field
    }

    private fun validateIndex(index: Int) {
        require(index in 1..536_870_911) { "Invalid field number: $index" }
        require(index !in 19_000..19_999) { "Reserved field number: $index" }
    }

    fun build() = Message(name, fields.values.toSet())
}