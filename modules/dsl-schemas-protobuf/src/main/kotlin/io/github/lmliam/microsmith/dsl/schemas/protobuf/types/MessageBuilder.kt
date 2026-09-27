package io.github.lmliam.microsmith.dsl.schemas.protobuf.types

import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Cardinality
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.CardinalityField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapFieldBuilder
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MessageField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ReferenceField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ReferenceFieldBuilder
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ScalarField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ScalarFieldBuilder
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.withCardinality
import io.github.lmliam.microsmith.dsl.schemas.protobuf.internal.allocation.IndexAllocator
import io.github.lmliam.microsmith.dsl.schemas.protobuf.internal.allocation.protobufReservedFieldIndexes
import io.github.lmliam.microsmith.dsl.schemas.protobuf.internal.names.NameRegistry
import io.github.lmliam.microsmith.dsl.schemas.protobuf.internal.reference.textualReference
import io.github.lmliam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import io.github.lmliam.microsmith.dsl.schemas.protobuf.oneof.OneofBuilder
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reference.ProtobufTypeRef
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.Max
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.MaxRange
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedBuilder
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.buildReservedDeclarations
import io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.field.MapFieldScope
import io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.field.ReferenceFieldScope
import io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.field.ScalarFieldScope
import io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.message.MessageScope
import io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.oneof.OneofScope
import io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.reserved.ReservedScope

internal class MessageBuilder(private val name: String) : MessageScope {
    private val allocator = IndexAllocator(1, protobufReservedFieldIndexes)
    private val nameRegistry = NameRegistry()

    private val fields = mutableMapOf<String, MessageField>()
    private val oneofs = mutableSetOf<Oneof>()

    fun build() =
        Message(
            name = name,
            fields = fields.values.sortedBy { it.index },
            oneofs = oneofs.sortedBy { it.name },
            reserved = buildReservedDeclarations(allocator, nameRegistry),
        )

    override fun optional(field: CardinalityField) {
        fields[field.name] = field.withCardinality(Cardinality.OPTIONAL)
    }

    override fun optional(block: MessageScope.() -> CardinalityField) {
        val field = this.block()
        fields[field.name] = field.withCardinality(Cardinality.OPTIONAL)
    }

    override fun repeated(field: CardinalityField) {
        fields[field.name] = field.withCardinality(Cardinality.REPEATED)
    }

    override fun repeated(block: MessageScope.() -> CardinalityField) {
        val field = this.block()
        fields[field.name] = field.withCardinality(Cardinality.REPEATED)
    }

    override fun oneof(name: String, block: OneofScope.() -> Unit) {
        val builder =
            OneofBuilder(
                    name,
                    allocator::allocate,
                    nameRegistry::use,
                )
                .apply(block)

        oneofs += builder.build()
    }

    override fun map(name: String, block: MapFieldScope.() -> Unit): MapField {
        require(name.isNotBlank()) { "Field name cannot be blank" }
        nameRegistry.validate(name)

        val builder = MapFieldBuilder().apply(block)

        val key = requireNotNull(builder.key) { "Map key type must be set" }
        val value = requireNotNull(builder.value) { "Map value type must be set" }

        val index = allocator.allocate(builder.index)
        nameRegistry.use(name)

        return MapField(name, index, MapType(key, value)).also { fields[name] = it }
    }

    override fun ref(name: String, target: String, block: ReferenceFieldScope.() -> Unit): ReferenceField =
        addReference(
            name,
            textualReference(target),
            block,
        )

    override fun ref(name: String, target: ProtobufTypeRef, block: ReferenceFieldScope.() -> Unit): ReferenceField =
        addReference(
            name,
            Reference.Symbolic(target),
            block,
        )

    override fun reserved(vararg indexes: Int) = indexes.forEach { allocator.reserve(it..it) }

    override fun reserved(vararg indexRanges: IntRange) = indexRanges.forEach { allocator.reserve(it) }

    override fun reserved(toMax: MaxRange) = allocator.reserve(toMax.from..Max.VALUE)

    override fun reserved(vararg names: String) = names.forEach { this.nameRegistry.reserve(it) }

    override fun reserved(block: ReservedScope.() -> Unit) {
        ReservedBuilder(allocator, nameRegistry).apply(block)
    }

    override fun int32(name: String, block: ScalarFieldScope.() -> Unit) = addField(name, PrimitiveType.INT32, block)

    override fun int64(name: String, block: ScalarFieldScope.() -> Unit) = addField(name, PrimitiveType.INT64, block)

    override fun uint32(name: String, block: ScalarFieldScope.() -> Unit) = addField(name, PrimitiveType.UINT32, block)

    override fun uint64(name: String, block: ScalarFieldScope.() -> Unit) = addField(name, PrimitiveType.UINT64, block)

    override fun sint32(name: String, block: ScalarFieldScope.() -> Unit) = addField(name, PrimitiveType.SINT32, block)

    override fun sint64(name: String, block: ScalarFieldScope.() -> Unit) = addField(name, PrimitiveType.SINT64, block)

    override fun fixed32(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveType.FIXED32, block)

    override fun fixed64(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveType.FIXED64, block)

    override fun sfixed32(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveType.SFIXED32, block)

    override fun sfixed64(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveType.SFIXED64, block)

    override fun float(name: String, block: ScalarFieldScope.() -> Unit) = addField(name, PrimitiveType.FLOAT, block)

    override fun double(name: String, block: ScalarFieldScope.() -> Unit) = addField(name, PrimitiveType.DOUBLE, block)

    override fun string(name: String, block: ScalarFieldScope.() -> Unit) = addField(name, PrimitiveType.STRING, block)

    override fun bytes(name: String, block: ScalarFieldScope.() -> Unit) = addField(name, PrimitiveType.BYTES, block)

    override fun bool(name: String, block: ScalarFieldScope.() -> Unit) = addField(name, PrimitiveType.BOOL, block)

    private fun addReference(
        name: String,
        reference: Reference,
        block: ReferenceFieldScope.() -> Unit,
    ): ReferenceField {
        nameRegistry.use(name)

        val (cardinality, index) =
            ReferenceFieldBuilder().apply(block).let { builder ->
                builder.cardinality to allocator.allocate(builder.index)
            }

        return ReferenceField(
                name,
                index,
                reference,
                cardinality,
            )
            .also { field -> fields[name] = field }
    }

    private fun addField(name: String, type: PrimitiveType, block: ScalarFieldScope.() -> Unit): ScalarField {
        nameRegistry.use(name)

        return ScalarFieldBuilder()
            .apply(block)
            .let { builder -> ScalarField(name, allocator.allocate(builder.index), type, builder.cardinality) }
            .also { field -> fields[name] = field }
    }
}
