package me.liam.microsmith.dsl.schemas.protobuf.message

import me.liam.microsmith.dsl.schemas.protobuf.MapFieldScope
import me.liam.microsmith.dsl.schemas.protobuf.MessageScope
import me.liam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import me.liam.microsmith.dsl.schemas.protobuf.oneof.OneofBuilder
import me.liam.microsmith.dsl.schemas.protobuf.OneofScope
import me.liam.microsmith.dsl.schemas.protobuf.ReservedScope
import me.liam.microsmith.dsl.schemas.protobuf.ScalarFieldScope
import me.liam.microsmith.dsl.schemas.protobuf.extensions.merge
import me.liam.microsmith.dsl.schemas.protobuf.field.Cardinality
import me.liam.microsmith.dsl.schemas.protobuf.field.Field
import me.liam.microsmith.dsl.schemas.protobuf.field.MapField
import me.liam.microsmith.dsl.schemas.protobuf.field.MapFieldBuilder
import me.liam.microsmith.dsl.schemas.protobuf.field.MapFieldType
import me.liam.microsmith.dsl.schemas.protobuf.field.PrimitiveFieldType
import me.liam.microsmith.dsl.schemas.protobuf.field.ScalarField
import me.liam.microsmith.dsl.schemas.protobuf.field.ScalarFieldBuilder
import me.liam.microsmith.dsl.schemas.protobuf.reserved.Max
import me.liam.microsmith.dsl.schemas.protobuf.reserved.MaxRange
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedBuilder
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedIndex
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedName
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedRange
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedToMax
import sun.security.util.KeyUtil.validate

class MessageBuilder(private val name: String) : MessageScope {
    private val fields = mutableMapOf<String, Field>()
    private val oneofs = mutableSetOf<Oneof>()
    private val usedIndexes = mutableSetOf<Int>()
    private val reservedIndexes = mutableSetOf<IntRange>()
    private val reservedNames = mutableSetOf<String>()
    private var nextIndex = 1

    fun build() = Message(
        name,
        fields.values.sortedBy { it.index },
        oneofs.sortedBy { it.name },
        reservedIndexes
            .sortedBy { it.first }
            .map { r ->
                when {
                    r.first == r.last -> ReservedIndex(r.first)
                    r.last == Max.VALUE -> ReservedToMax(r.first)
                    else -> ReservedRange(r)
                }
            } +
                reservedNames.sorted().map { ReservedName(it) }

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
            ::validate
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

    override fun reserved(vararg indexes: Int) =
        indexes.forEach { reserveRange(it..it) }

    override fun reserved(vararg indexRanges: IntRange) =
        indexRanges.forEach { reserveRange(it) }

    override fun reserved(toMax: MaxRange) =
        reserveRange(toMax.from..Max.VALUE)

    override fun reserved(vararg names: String) =
        names.forEach { reserveName(it) }

    override fun reserved(block: ReservedScope.() -> Unit) {
        val builder = ReservedBuilder().apply(block)
        builder.reservedIndexes.forEach { reserveRange(it) }
        builder.reservedNames.forEach { reserveName(it) }
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

    private fun allocateIndex(idx: Int? = null): Int =
        if (idx != null) {
            validate(idx)
            usedIndexes += idx
            idx
        } else {
            val candidate = generateSequence(nextIndex) { it + 1 }
                .first { c ->
                    c !in usedIndexes &&
                            c !in protoReservedIndexes &&
                            reservedIndexes.none { c in it }
                }
            validate(candidate)
            usedIndexes += candidate
            nextIndex = candidate + 1
            candidate
        }

    private fun addField(
        name: String,
        type: PrimitiveFieldType,
        block: ScalarFieldScope.() -> Unit
    ): ScalarField {
        validate(name)

        return ScalarFieldBuilder()
            .apply(block)
            .let { builder ->
                ScalarField(name, allocateIndex(builder.index), type, builder.cardinality)
            }
            .also { field -> fields[name] = field }
    }

    private fun validateIndex(index: Int) {
        require(index in 1..Max.VALUE) { "Invalid field number: $index" }
    }

    private fun validate(index: Int) {
        validateIndex(index)

        require(index !in usedIndexes) {
            "Field number $index already used"
        }
        require(index < protoReservedIndexes.first || index > protoReservedIndexes.last) {
            "Field number $index is in the proto reserved range"
        }
        require(reservedIndexes.none { index in it }) {
            "Field number $index is already reserved"
        }
    }

    private fun validate(range: IntRange) {
        validateIndex(range.first)
        validateIndex(range.last)

        require(usedIndexes.none { it in range }) {
            "Range $range overlaps with used field numbers"
        }
        require(range.last < protoReservedIndexes.first || range.first > protoReservedIndexes.last) {
            "Range $range overlaps with proto reserved numbers"
        }
        require(reservedIndexes.none { existing ->
            existing.first <= range.last && range.first <= existing.last
        }) {
            "Range $range overlaps with already reserved numbers"
        }
    }

    private fun validate(name: String) {
        require(name.isNotBlank()) { "Field name cannot be blank" }
        require(name !in fields.keys) { "Cannot reserve used field name: $name" }
        require(name !in oneofs.flatMap { it.fields.map { f -> f.name } }) {
            "Cannot reserve used field name in oneof: $name"
        }
        require(name !in reservedNames) { "Field name already reserved: $name" }
    }

    private fun reserveRange(range: IntRange) {
        validate(range)
        reservedIndexes.merge(range)
    }

    private fun reserveName(name: String) {
        validate(name)
        reservedNames += name
    }

    companion object {
        private val protoReservedIndexes = 19_000..19_999
    }
}