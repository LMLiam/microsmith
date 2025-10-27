package me.liam.microsmith.dsl.schemas.protobuf

class OneofBuilder(
    private val name: String,
    private var fieldIndex: Int,
    private val usedIndexes: MutableSet<Int>
) : OneofScope {
    private val fields = mutableMapOf<String, Field>()

    override fun int32(name: String, block: OneofFieldScope.() -> Unit): Field = addField(name, FieldType.INT32, block)
    override fun int64(name: String, block: OneofFieldScope.() -> Unit): Field = addField(name, FieldType.INT64, block)
    override fun uint32(name: String, block: OneofFieldScope.() -> Unit): Field = addField(name, FieldType.UINT32, block)
    override fun uint64(name: String, block: OneofFieldScope.() -> Unit): Field = addField(name, FieldType.UINT64, block)
    override fun sint32(name: String, block: OneofFieldScope.() -> Unit): Field = addField(name, FieldType.SINT32, block)
    override fun sint64(name: String, block: OneofFieldScope.() -> Unit): Field = addField(name, FieldType.SINT64, block)
    override fun fixed32(name: String, block: OneofFieldScope.() -> Unit): Field = addField(name, FieldType.FIXED32, block)
    override fun fixed64(name: String, block: OneofFieldScope.() -> Unit): Field = addField(name, FieldType.FIXED64, block)
    override fun sfixed32(name: String, block: OneofFieldScope.() -> Unit): Field = addField(name, FieldType.SFIXED32, block)
    override fun sfixed64(name: String, block: OneofFieldScope.() -> Unit): Field = addField(name, FieldType.SFIXED64, block)
    override fun float(name: String, block: OneofFieldScope.() -> Unit): Field = addField(name, FieldType.FLOAT, block)
    override fun double(name: String, block: OneofFieldScope.() -> Unit): Field = addField(name, FieldType.DOUBLE, block)
    override fun string(name: String, block: OneofFieldScope.() -> Unit): Field = addField(name, FieldType.STRING, block)
    override fun bytes(name: String, block: OneofFieldScope.() -> Unit): Field = addField(name, FieldType.BYTES, block)
    override fun bool(name: String, block: OneofFieldScope.() -> Unit): Field = addField(name, FieldType.BOOL, block)

    private fun addField(name: String, type: FieldType, block: FieldScope.() -> Unit): Field {
        require(!fields.containsKey(name)) { "Duplicate field in oneof: $name" }

        val builder = FieldBuilder(fieldIndex).apply(block)

        require(builder.cardinality == Cardinality.REQUIRED) {
            "Oneof fields cannot be optional or repeated"
        }

        // validate against global usedIndexes
        require(builder.index !in usedIndexes) { "Duplicate field number: ${builder.index}" }
        validateIndex(builder.index)

        val field = Field(name, type, builder.index, Cardinality.REQUIRED)
        fields[name] = field
        usedIndexes += builder.index

        fieldIndex = maxOf(fieldIndex + 1, builder.index + 1)
        return field
    }

    private fun validateIndex(index: Int) {
        require(index in 1..536_870_911) { "Invalid field number: $index" }
        require(index !in 19_000..19_999) { "Reserved field number: $index" }
    }

    fun build() = Oneof(name, fields.values.toSet())
}