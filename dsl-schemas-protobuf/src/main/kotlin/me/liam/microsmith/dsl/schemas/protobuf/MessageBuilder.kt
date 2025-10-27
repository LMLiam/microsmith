package me.liam.microsmith.dsl.schemas.protobuf

class MessageBuilder(private val name: String) : MessageScope {
    private val fields = mutableMapOf<String, Field>()
    private val oneofs = mutableSetOf<Oneof>()
    private val usedIndexes = mutableSetOf<Int>()
    private var nextIndex = 1

    internal fun allocateIndex(requested: Int? = null): Int {
        return if (requested != null) {
            validateIndex(requested)
            require(requested !in reservedIndexes) { "Reserved field number: $requested" }
            require(requested !in usedIndexes) { "Duplicate field number: $requested" }
            usedIndexes += requested
            requested
        } else {
            var candidate = nextIndex
            if (candidate in reservedIndexes) {
                candidate = reservedIndexes.last + 1
            }
            while (candidate in usedIndexes) {
                candidate++
                if (candidate in reservedIndexes) {
                    candidate = reservedIndexes.last + 1
                }
            }
            validateIndex(candidate)
            usedIndexes += candidate
            nextIndex = candidate + 1
            candidate
        }
    }

    override fun optional(field: Field) {
        require(field.cardinality == Cardinality.REQUIRED) { "Field cardinality already set to ${field.cardinality}" }
        fields[field.name] = field.copy(cardinality = Cardinality.OPTIONAL)
    }

    override fun optional(block: MessageScope.() -> Field) {
        val field = this.block()
        require(field.cardinality == Cardinality.REQUIRED) { "Field cardinality already set to ${field.cardinality}" }
        val updated = field.copy(cardinality = Cardinality.OPTIONAL)
        fields[field.name] = updated
    }

    override fun repeated(field: Field) {
        require(field.cardinality == Cardinality.REQUIRED) { "Field cardinality already set to ${field.cardinality}" }
        fields[field.name] = field.copy(cardinality = Cardinality.REPEATED)
    }

    override fun repeated(block: MessageScope.() -> Field) {
        val field = this.block()
        require(field.cardinality == Cardinality.REQUIRED) { "Field cardinality already set to ${field.cardinality}" }
        val updated = field.copy(cardinality = Cardinality.REPEATED)
        fields[field.name] = updated
    }

    override fun oneof(name: String, block: OneofScope.() -> Unit) {
        val builder = OneofBuilder(name, this::allocateIndex).apply(block)
        oneofs += builder.build()
    }

    override fun int32(name: String, block: FieldScope.() -> Unit) = addField(name, FieldType.INT32, block)
    override fun int64(name: String, block: FieldScope.() -> Unit) = addField(name, FieldType.INT64, block)
    override fun uint32(name: String, block: FieldScope.() -> Unit) = addField(name, FieldType.UINT32, block)
    override fun uint64(name: String, block: FieldScope.() -> Unit) = addField(name, FieldType.UINT64, block)
    override fun sint32(name: String, block: FieldScope.() -> Unit) = addField(name, FieldType.SINT32, block)
    override fun sint64(name: String, block: FieldScope.() -> Unit) = addField(name, FieldType.SINT64, block)
    override fun fixed32(name: String, block: FieldScope.() -> Unit) = addField(name, FieldType.FIXED32, block)
    override fun fixed64(name: String, block: FieldScope.() -> Unit) = addField(name, FieldType.FIXED64, block)
    override fun sfixed32(name: String, block: FieldScope.() -> Unit) = addField(name, FieldType.SFIXED32, block)
    override fun sfixed64(name: String, block: FieldScope.() -> Unit) = addField(name, FieldType.SFIXED64, block)
    override fun float(name: String, block: FieldScope.() -> Unit) = addField(name, FieldType.FLOAT, block)
    override fun double(name: String, block: FieldScope.() -> Unit) = addField(name, FieldType.DOUBLE, block)
    override fun string(name: String, block: FieldScope.() -> Unit) = addField(name, FieldType.STRING, block)
    override fun bytes(name: String, block: FieldScope.() -> Unit) = addField(name, FieldType.BYTES, block)
    override fun bool(name: String, block: FieldScope.() -> Unit) = addField(name, FieldType.BOOL, block)

    private fun addField(name: String, type: FieldType, block: FieldScope.() -> Unit): Field {
        require(name.isNotBlank()) { "Field name cannot be blank" }
        require(!fields.containsKey(name)) { "Duplicate field name: $name" }

        val builder = FieldBuilder(nextIndex).apply(block)
        val index = allocateIndex(builder.index.takeIf { it != 0 }) // 0 means “use default”

        val field = Field(name, type, index, builder.cardinality)
        fields[name] = field
        return field
    }

    private fun validateIndex(index: Int) {
        require(index in 1..536_870_911) { "Invalid field number: $index" }
    }

    fun build() = Message(name, fields.values.toSet(), oneofs.toSet())

    companion object {
        private val reservedIndexes = 19_000..19_999
    }
}