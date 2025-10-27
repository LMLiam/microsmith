package me.liam.microsmith.dsl.schemas.protobuf.oneof

import me.liam.microsmith.dsl.schemas.protobuf.OneofFieldScope
import me.liam.microsmith.dsl.schemas.protobuf.OneofScope
import me.liam.microsmith.dsl.schemas.protobuf.field.OneofField
import me.liam.microsmith.dsl.schemas.protobuf.field.OneofFieldBuilder
import me.liam.microsmith.dsl.schemas.protobuf.field.PrimitiveFieldType

class OneofBuilder(
    private val name: String,
    private val allocateIndex: (Int?) -> Int,
    private val checkNameConflict: (String) -> Unit
) : OneofScope {
    private val fields = mutableMapOf<String, OneofField>()

    override fun int32(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveFieldType.INT32, block)

    override fun int64(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveFieldType.INT64, block)

    override fun uint32(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveFieldType.UINT32, block)

    override fun uint64(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveFieldType.UINT64, block)

    override fun sint32(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveFieldType.SINT32, block)

    override fun sint64(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveFieldType.SINT64, block)

    override fun fixed32(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveFieldType.FIXED32, block)

    override fun fixed64(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveFieldType.FIXED64, block)

    override fun sfixed32(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveFieldType.SFIXED32, block)

    override fun sfixed64(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveFieldType.SFIXED64, block)

    override fun float(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveFieldType.FLOAT, block)

    override fun double(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveFieldType.DOUBLE, block)

    override fun string(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveFieldType.STRING, block)

    override fun bytes(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveFieldType.BYTES, block)

    override fun bool(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveFieldType.BOOL, block)

    private fun addField(name: String, type: PrimitiveFieldType, block: OneofFieldScope.() -> Unit): OneofField {
        require(!fields.containsKey(name)) { "Duplicate field in oneof: $name" }
        checkNameConflict(name)

        val builder = OneofFieldBuilder().apply(block)
        val index = allocateIndex(builder.index)

        val field = OneofField(name, index, type)
        fields[name] = field
        return field
    }

    fun build() = Oneof(name, fields.values.toSet())
}