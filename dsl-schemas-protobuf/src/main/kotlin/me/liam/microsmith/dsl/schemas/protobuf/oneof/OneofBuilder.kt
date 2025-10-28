package me.liam.microsmith.dsl.schemas.protobuf.oneof

import me.liam.microsmith.dsl.schemas.protobuf.OneofFieldScope
import me.liam.microsmith.dsl.schemas.protobuf.OneofReferenceFieldScope
import me.liam.microsmith.dsl.schemas.protobuf.OneofScope
import me.liam.microsmith.dsl.schemas.protobuf.field.*
import me.liam.microsmith.dsl.schemas.protobuf.support.getReferencePath

class OneofBuilder(
    private val name: String,
    private val segments: List<String>,
    private val allocateIndex: (Int?) -> Int,
    private val useName: (String) -> Unit
) : OneofScope {
    private val fields = mutableMapOf<String, OneofField>()

    override fun ref(
        name: String,
        target: String,
        block: OneofReferenceFieldScope.() -> Unit
    ): OneofField {
        useName(name)

        val fqName = getReferencePath(segments, target).joinToString(".")

        val index = ReferenceFieldBuilder()
            .apply(block)
            .let { allocateIndex(it.index) }

        return OneofField(name, index, Reference(fqName))
            .also { fields[name] = it }
    }

    override fun int32(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveType.INT32, block)

    override fun int64(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveType.INT64, block)

    override fun uint32(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveType.UINT32, block)

    override fun uint64(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveType.UINT64, block)

    override fun sint32(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveType.SINT32, block)

    override fun sint64(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveType.SINT64, block)

    override fun fixed32(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveType.FIXED32, block)

    override fun fixed64(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveType.FIXED64, block)

    override fun sfixed32(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveType.SFIXED32, block)

    override fun sfixed64(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveType.SFIXED64, block)

    override fun float(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveType.FLOAT, block)

    override fun double(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveType.DOUBLE, block)

    override fun string(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveType.STRING, block)

    override fun bytes(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveType.BYTES, block)

    override fun bool(name: String, block: OneofFieldScope.() -> Unit): OneofField =
        addField(name, PrimitiveType.BOOL, block)

    private fun addField(
        name: String,
        type: PrimitiveType,
        block: OneofFieldScope.() -> Unit
    ): OneofField {
        require(name !in fields) { "Duplicate field in oneof: $name" }
        useName(name)

        val index = OneofFieldBuilder()
            .apply(block)
            .let { allocateIndex(it.index) }

        return OneofField(name, index, type)
            .also { fields[name] = it }
    }

    fun build() = Oneof(name, fields.values.sortedBy { it.index })
}