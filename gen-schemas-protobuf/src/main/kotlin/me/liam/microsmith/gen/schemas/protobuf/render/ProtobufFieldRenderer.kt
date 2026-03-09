package me.liam.microsmith.gen.schemas.protobuf.render

import me.liam.microsmith.dsl.schemas.protobuf.field.Cardinality
import me.liam.microsmith.dsl.schemas.protobuf.field.CardinalityField
import me.liam.microsmith.dsl.schemas.protobuf.field.Field
import me.liam.microsmith.dsl.schemas.protobuf.field.MapField
import me.liam.microsmith.dsl.schemas.protobuf.field.MapType
import me.liam.microsmith.dsl.schemas.protobuf.field.OneofField
import me.liam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import me.liam.microsmith.dsl.schemas.protobuf.field.Reference
import me.liam.microsmith.dsl.schemas.protobuf.field.ReferenceField
import me.liam.microsmith.dsl.schemas.protobuf.field.ScalarField
import me.liam.microsmith.dsl.schemas.protobuf.field.ValueType
import me.liam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import me.liam.microsmith.dsl.schemas.protobuf.types.EnumValue
import me.liam.microsmith.gen.schemas.protobuf.emission.invalidTopLevelOneofField
import me.liam.microsmith.gen.schemas.protobuf.emission.invariantViolation

internal object ProtobufFieldRenderer {
    private val primitiveTypeRenderings =
        mapOf(
            PrimitiveType.INT32 to "int32",
            PrimitiveType.INT64 to "int64",
            PrimitiveType.UINT32 to "uint32",
            PrimitiveType.UINT64 to "uint64",
            PrimitiveType.SINT32 to "sint32",
            PrimitiveType.SINT64 to "sint64",
            PrimitiveType.FIXED32 to "fixed32",
            PrimitiveType.FIXED64 to "fixed64",
            PrimitiveType.SFIXED32 to "sfixed32",
            PrimitiveType.SFIXED64 to "sfixed64",
            PrimitiveType.STRING to "string",
            PrimitiveType.BOOL to "bool",
            PrimitiveType.FLOAT to "float",
            PrimitiveType.DOUBLE to "double",
            PrimitiveType.BYTES to "bytes",
        )

    fun render(field: Field): String = buildString {
        val prefix = (field as? CardinalityField)?.cardinality?.let(::renderPrefix).orEmpty()
        append(prefix)
        when (field) {
            is ScalarField -> append("${renderValueType(field.primitive)} ${field.name} = ${field.index};")
            is ReferenceField -> append("${field.reference.name} ${field.name} = ${field.index};")
            is MapField -> append("${render(field.type)} ${field.name} = ${field.index};")
            is OneofField -> invalidTopLevelOneofField(field.name)
        }
    }

    fun render(oneof: Oneof): String = buildString {
        appendLine("oneof ${oneof.name} {")
        oneof.fields.forEach { appendLine(render(it).prependIndent("  ")) }
        append("}")
    }

    fun render(field: OneofField): String = "${renderValueType(field.fieldType)} ${field.name} = ${field.index};"

    fun render(value: EnumValue): String = "${value.name} = ${value.index};"

    private fun render(type: MapType): String = "map<${renderValueType(type.key)}, ${renderValueType(type.value)}>"

    private fun renderPrefix(cardinality: Cardinality): String = when (cardinality) {
        Cardinality.REQUIRED -> ""
        Cardinality.OPTIONAL -> "optional "
        Cardinality.REPEATED -> "repeated "
    }

    private fun renderValueType(type: PrimitiveType): String =
        primitiveTypeRenderings[type] ?: invariantViolation("Unsupported protobuf primitive type: $type")

    private fun renderValueType(type: ValueType): String = when (type) {
        is PrimitiveType -> renderValueType(type)
        is Reference -> type.name
    }
}
