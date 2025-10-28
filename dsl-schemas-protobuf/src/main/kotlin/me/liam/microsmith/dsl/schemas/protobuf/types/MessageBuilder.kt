package me.liam.microsmith.dsl.schemas.protobuf.types

import me.liam.microsmith.dsl.schemas.protobuf.*
import me.liam.microsmith.dsl.schemas.protobuf.field.*
import me.liam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import me.liam.microsmith.dsl.schemas.protobuf.oneof.OneofBuilder
import me.liam.microsmith.dsl.schemas.protobuf.reserved.*
import me.liam.microsmith.dsl.schemas.protobuf.support.IndexAllocator
import me.liam.microsmith.dsl.schemas.protobuf.support.NameRegistry
import me.liam.microsmith.dsl.schemas.protobuf.support.getReferencePath

class MessageBuilder(
    private val name: String,
    private val segments: List<String>
) : MessageScope {
    private val allocator = IndexAllocator(1, protoReservedIndexes)
    private val nameRegistry = NameRegistry()

    private val fields = mutableMapOf<String, Field>()
    private val oneofs = mutableSetOf<Oneof>()

    fun build() = Message(
        name = name,
        fields = fields.values.sortedBy { it.index },
        oneofs = oneofs.sortedBy { it.name },
        reserved = buildList {
            allocator.reserved()
                .sortedBy { it.first }
                .mapTo(this, Reserved::fromRange)

            nameRegistry.reserved()
                .sorted()
                .mapTo(this, ::ReservedName)
        }
    )

    override fun optional(field: ScalarField) {
        require(field.cardinality == Cardinality.REQUIRED) { "Field cardinality already set to ${field.cardinality}" }
        fields[field.name] = field.copy(cardinality = Cardinality.OPTIONAL)
    }

    override fun optional(field: ReferenceField) {
        require(field.cardinality == Cardinality.REQUIRED) { "Field cardinality already set to ${field.cardinality}" }
        fields[field.name] = field.copy(cardinality = Cardinality.OPTIONAL)
    }

    override fun optional(block: MessageScope.() -> ScalarField) {
        val field = this.block()
        require(field.cardinality == Cardinality.REQUIRED) { "Field cardinality already set to ${field.cardinality}" }
        val updated = field.copy(cardinality = Cardinality.OPTIONAL)
        fields[field.name] = updated
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("optionalRef")
    override fun optional(blockRef: MessageScope.() -> ReferenceField) {
        val field = this.blockRef()
        require(field.cardinality == Cardinality.REQUIRED) { "Field cardinality already set to ${field.cardinality}" }
        val updated = field.copy(cardinality = Cardinality.OPTIONAL)
        fields[field.name] = updated
    }

    override fun repeated(field: ScalarField) {
        require(field.cardinality == Cardinality.REQUIRED) { "Field cardinality already set to ${field.cardinality}" }
        fields[field.name] = field.copy(cardinality = Cardinality.REPEATED)
    }

    override fun repeated(field: ReferenceField) {
        require(field.cardinality == Cardinality.REQUIRED) { "Field cardinality already set to ${field.cardinality}" }
        fields[field.name] = field.copy(cardinality = Cardinality.REPEATED)
    }

    override fun repeated(block: MessageScope.() -> ScalarField) {
        val field = this.block()
        require(field.cardinality == Cardinality.REQUIRED) { "Field cardinality already set to ${field.cardinality}" }
        val updated = field.copy(cardinality = Cardinality.REPEATED)
        fields[field.name] = updated
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("repeatedRef")
    override fun repeated(blockRef: MessageScope.() -> ReferenceField) {
        val field = this.blockRef()
        require(field.cardinality == Cardinality.REQUIRED) { "Field cardinality already set to ${field.cardinality}" }
        val updated = field.copy(cardinality = Cardinality.REPEATED)
        fields[field.name] = updated
    }

    override fun oneof(name: String, block: OneofScope.() -> Unit) {
        val builder = OneofBuilder(
            name,
            segments,
            ::allocateIndex,
            nameRegistry::use
        ).apply(block)

        oneofs += builder.build()
    }

    override fun map(
        name: String,
        block: MapFieldScope.() -> Unit
    ): MapField {
        require(name.isNotBlank()) { "Field name cannot be blank" }
        require(name !in fields) { "Duplicate field name: $name" }

        val builder = MapFieldBuilder(segments).apply(block)

        val key = requireNotNull(builder.key) { "Map key type must be set" }
        val value = requireNotNull(builder.value) { "Map value type must be set" }

        val index = allocateIndex(builder.index)

        return MapField(name, index, MapType(key, value))
            .also { fields[name] = it }
    }

    override fun ref(
        name: String,
        target: String,
        block: ReferenceFieldScope.() -> Unit
    ): ReferenceField {
        nameRegistry.use(name)

        val fqName = getReferencePath(segments, target).joinToString(".")

        val (cardinality, index) = ReferenceFieldBuilder()
            .apply(block)
            .let { it.cardinality to allocateIndex(it.index) }

        return ReferenceField(name, index, Reference(fqName), cardinality)
            .also { fields[name] = it }
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

    override fun int32(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveType.INT32, block)

    override fun int64(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveType.INT64, block)

    override fun uint32(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveType.UINT32, block)

    override fun uint64(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveType.UINT64, block)

    override fun sint32(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveType.SINT32, block)

    override fun sint64(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveType.SINT64, block)

    override fun fixed32(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveType.FIXED32, block)

    override fun fixed64(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveType.FIXED64, block)

    override fun sfixed32(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveType.SFIXED32, block)

    override fun sfixed64(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveType.SFIXED64, block)

    override fun float(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveType.FLOAT, block)

    override fun double(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveType.DOUBLE, block)

    override fun string(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveType.STRING, block)

    override fun bytes(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveType.BYTES, block)

    override fun bool(name: String, block: ScalarFieldScope.() -> Unit) = addField(name, PrimitiveType.BOOL, block)

    private fun allocateIndex(idx: Int? = null): Int = allocator.allocate(idx)

    private fun addField(
        name: String,
        type: PrimitiveType,
        block: ScalarFieldScope.() -> Unit
    ): ScalarField {
        nameRegistry.use(name)

        return ScalarFieldBuilder()
            .apply(block)
            .let { builder ->
                ScalarField(name, allocateIndex(builder.index), type, builder.cardinality)
            }
            .also { field -> fields[name] = field }
    }

    companion object {
        private val protoReservedIndexes = 19_000..19_999
    }
}