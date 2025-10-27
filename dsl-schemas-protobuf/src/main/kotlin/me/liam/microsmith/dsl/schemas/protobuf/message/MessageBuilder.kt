package me.liam.microsmith.dsl.schemas.protobuf.message

import me.liam.microsmith.dsl.schemas.protobuf.MapFieldScope
import me.liam.microsmith.dsl.schemas.protobuf.MessageScope
import me.liam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import me.liam.microsmith.dsl.schemas.protobuf.oneof.OneofBuilder
import me.liam.microsmith.dsl.schemas.protobuf.OneofScope
import me.liam.microsmith.dsl.schemas.protobuf.ScalarFieldScope
import me.liam.microsmith.dsl.schemas.protobuf.field.Cardinality
import me.liam.microsmith.dsl.schemas.protobuf.field.Field
import me.liam.microsmith.dsl.schemas.protobuf.field.MapField
import me.liam.microsmith.dsl.schemas.protobuf.field.MapFieldBuilder
import me.liam.microsmith.dsl.schemas.protobuf.field.MapFieldType
import me.liam.microsmith.dsl.schemas.protobuf.field.PrimitiveFieldType
import me.liam.microsmith.dsl.schemas.protobuf.field.ScalarField
import me.liam.microsmith.dsl.schemas.protobuf.field.ScalarFieldBuilder
import me.liam.microsmith.dsl.schemas.protobuf.reserved.Reserved
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedIndex
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedName
import me.liam.microsmith.dsl.schemas.protobuf.reserved.combineReservedIndexes

class MessageBuilder(private val name: String) : MessageScope {
    private val fields = mutableMapOf<String, Field>()
    private val oneofs = mutableSetOf<Oneof>()
    private val usedIndexes = mutableSetOf<Int>()
    private val reservedIndexes = mutableSetOf<Int>()
    private val reservedNames = mutableSetOf<String>()
    private var nextIndex = 1

    internal fun allocateIndex(requested: Int? = null): Int {
        return if (requested != null) {
            validateIndex(requested)
            require(requested !in protoReservedIndexes) { "Reserved field number: $requested" }
            require(requested !in usedIndexes) { "Duplicate field number: $requested" }
            require(requested !in reservedIndexes) { "Reserved field number: $requested" }
            usedIndexes += requested
            requested
        } else {
            var candidate = nextIndex
            if (candidate in protoReservedIndexes) {
                candidate = protoReservedIndexes.last + 1
            }
            while (candidate in usedIndexes || candidate in protoReservedIndexes || candidate in reservedIndexes) {
                candidate++
            }
            validateIndex(candidate)
            usedIndexes += candidate
            nextIndex = candidate + 1
            candidate
        }
    }

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
            checkNameConflict = { candidate ->
                // Check against top-level fields
                require(candidate !in fields.keys) {
                    "Duplicate field name in oneof: $candidate"
                }
                // Check against all existing oneof fields
                val existingOneofNames = oneofs.flatMap { it.fields.map { name } }
                require(candidate !in existingOneofNames) {
                    "Duplicate field name across oneofs: $candidate"
                }
            }
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

        return MapField(name, index, MapFieldType(key, value)).also {
            fields[name] = it
        }
    }

    override fun reserved(vararg indexes: Int) {
        indexes.forEach {
            validateIndex(it)
            require(it !in usedIndexes) { "Cannot reserve used field number: $it" }
            require(it !in reservedIndexes) { "Cannot reserve reserved field number: $it" }
            require(it !in protoReservedIndexes) { "Cannot reserve proto reserved field number: $it" }
            reservedIndexes += it
        }
    }

    override fun reserved(vararg names: String) {
        names.forEach {
            require(it !in fields.keys) { "Cannot reserve used field name: $it" }
            require(it !in oneofs.flatMap { fields.map { name } }) { "Cannot reserve used field name in oneof: $it" }
            require(it !in reservedNames) { "Cannot reserve reserved field name: $it" }
            reservedNames += it
        }
    }

    override fun int32(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveFieldType.INT32, block)

    override fun int64(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveFieldType.INT64, block)

    override fun uint32(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveFieldType.UINT32, block)

    override fun uint64(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveFieldType.UINT64, block)

    override fun sint32(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveFieldType.SINT32, block)

    override fun sint64(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveFieldType.SINT64, block)

    override fun fixed32(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveFieldType.FIXED32, block)

    override fun fixed64(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveFieldType.FIXED64, block)

    override fun sfixed32(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveFieldType.SFIXED32, block)

    override fun sfixed64(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveFieldType.SFIXED64, block)

    override fun float(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveFieldType.FLOAT, block)

    override fun double(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveFieldType.DOUBLE, block)

    override fun string(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveFieldType.STRING, block)

    override fun bytes(name: String, block: ScalarFieldScope.() -> Unit) =
        addField(name, PrimitiveFieldType.BYTES, block)

    override fun bool(name: String, block: ScalarFieldScope.() -> Unit) = addField(name, PrimitiveFieldType.BOOL, block)

    private fun addField(name: String, type: PrimitiveFieldType, block: ScalarFieldScope.() -> Unit): ScalarField {
        require(name.isNotBlank()) { "Field name cannot be blank" }
        require(name !in fields.keys) { "Duplicate field name: $name" }
        require(name !in oneofs.flatMap { fields.map { name } }) { "Duplicate field name in oneof: $name" }
        require(name !in reservedNames) { "Duplicate field name in reserved names: $name" }

        val builder = ScalarFieldBuilder().apply(block)
        val index = allocateIndex(builder.index)

        val field = ScalarField(name, index, type, builder.cardinality)
        fields[name] = field
        return field
    }

    private fun validateIndex(index: Int) {
        require(index in 1..536_870_911) { "Invalid field number: $index" }
    }

    fun build() = Message(
        name,
        fields.values.toSet(),
        oneofs.toSet(),
        (
                combineReservedIndexes(reservedIndexes) +
                        reservedNames.map { ReservedName(it) }
                ).toSet(),
    )

    companion object {
        private val protoReservedIndexes = 19_000..19_999
    }
}