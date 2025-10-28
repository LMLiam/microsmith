package me.liam.microsmith.dsl.schemas.protobuf.types

import me.liam.microsmith.dsl.schemas.protobuf.MapFieldScope
import me.liam.microsmith.dsl.schemas.protobuf.MessageScope
import me.liam.microsmith.dsl.schemas.protobuf.OneofScope
import me.liam.microsmith.dsl.schemas.protobuf.ReservedScope
import me.liam.microsmith.dsl.schemas.protobuf.ScalarFieldScope
import me.liam.microsmith.dsl.schemas.protobuf.field.*
import me.liam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import me.liam.microsmith.dsl.schemas.protobuf.oneof.OneofBuilder
import me.liam.microsmith.dsl.schemas.protobuf.reserved.*
import me.liam.microsmith.dsl.schemas.protobuf.support.IndexAllocator
import me.liam.microsmith.dsl.schemas.protobuf.support.NameRegistry

class MessageBuilder(private val name: String) : MessageScope {
    private val allocator = IndexAllocator(1, protoReservedIndexes)
    private val nameRegistry = NameRegistry()

    private val fields = mutableMapOf<String, Field>()
    private val oneofs = mutableSetOf<Oneof>()

    fun build() = Message(
        name,
        fields.values.sortedBy { it.index },
        oneofs.sortedBy { it.name },
        allocator.reserved()
            .sortedBy { it.first }
            .map(Reserved::fromRange) +
                nameRegistry.reserved().sorted().map(::ReservedName)
    )

    override fun optional(field: ScalarField) {
        require(field.cardinality == Cardinality.REQUIRED) { "Field cardinality already set to ${field.cardinality}" }
        fields[field.name] = field.copy(cardinality = Cardinality.OPTIONAL)
    }

    override fun optional(block: MessageScope.() -> ScalarField) {
        val field = this.block()
        require(field.cardinality == Cardinality.REQUIRED) { "Field cardinality already set to ${field.cardinality}" }
        val updated = field.copy(cardinality = Cardinality.OPTIONAL)
        fields[field.name] = updated
    }

    override fun repeated(field: ScalarField) {
        require(field.cardinality == Cardinality.REQUIRED) { "Field cardinality already set to ${field.cardinality}" }
        fields[field.name] = field.copy(cardinality = Cardinality.REPEATED)
    }

    override fun repeated(block: MessageScope.() -> ScalarField) {
        val field = this.block()
        require(field.cardinality == Cardinality.REQUIRED) { "Field cardinality already set to ${field.cardinality}" }
        val updated = field.copy(cardinality = Cardinality.REPEATED)
        fields[field.name] = updated
    }

    override fun oneof(name: String, block: OneofScope.() -> Unit) {
        val builder = OneofBuilder(
            name,
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

        val builder = MapFieldBuilder().apply(block)

        val key = builder.key
        val value = builder.value

        requireNotNull(key) { "Map key type must be set" }
        requireNotNull(value) { "Map value type must be set" }

        val index = allocateIndex(builder.index)

        return MapField(name, index, MapType(key, value)).also {
            fields[name] = it
        }
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