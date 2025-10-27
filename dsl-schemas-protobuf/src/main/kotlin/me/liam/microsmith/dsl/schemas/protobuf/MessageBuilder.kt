package me.liam.microsmith.dsl.schemas.protobuf

class MessageBuilder(private val name: String) : MessageScope {
    private val fields = mutableSetOf<Field>()
    private var fieldNumber = 1

    override fun int32(name: String, block: FieldScope.() -> Unit) {
        val builder = FieldBuilder(fieldNumber, false).apply(block)
        addField(name, builder, FieldType.INT32)
    }

    override fun string(name: String, block: FieldScope.() -> Unit) {
        val builder = FieldBuilder(fieldNumber, false).apply(block)
        addField(name, builder, FieldType.STRING)
    }

    private fun addField(name: String, builder: FieldBuilder, type: FieldType) {
        fields += Field(name, type, builder.index, builder.optional)
        fieldNumber++
    }

    fun build() = Message(name, fields)
}