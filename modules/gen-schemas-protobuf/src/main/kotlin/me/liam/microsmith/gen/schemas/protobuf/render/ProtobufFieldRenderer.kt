package me.liam.microsmith.gen.schemas.protobuf.render

import me.liam.microsmith.dsl.schemas.protobuf.field.Cardinality
import me.liam.microsmith.dsl.schemas.protobuf.field.CardinalityField
import me.liam.microsmith.dsl.schemas.protobuf.field.Field
import me.liam.microsmith.dsl.schemas.protobuf.field.MapField
import me.liam.microsmith.dsl.schemas.protobuf.field.OneofField
import me.liam.microsmith.dsl.schemas.protobuf.field.ReferenceField
import me.liam.microsmith.dsl.schemas.protobuf.field.ScalarField
import me.liam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import me.liam.microsmith.dsl.schemas.protobuf.types.EnumValue
import me.liam.microsmith.gen.schemas.protobuf.emission.invalidTopLevelOneofField

internal object ProtobufFieldRenderer {
    fun render(field: Field): String = buildString {
        val prefix = (field as? CardinalityField)?.cardinality?.let(::renderPrefix).orEmpty()
        append(prefix)
        when (field) {
            is ScalarField -> append(
                "${ProtobufValueTypeRenderer.render(field.primitive)} ${field.name} = ${field.index};",
            )
            is ReferenceField -> append("${field.reference.name} ${field.name} = ${field.index};")
            is MapField -> append("${ProtobufValueTypeRenderer.render(field.type)} ${field.name} = ${field.index};")
            is OneofField -> invalidTopLevelOneofField(field.name)
        }
    }

    fun render(oneof: Oneof): String = buildString {
        appendLine("oneof ${oneof.name} {")
        oneof.fields.forEach { appendLine(render(it).prependIndent("  ")) }
        append("}")
    }

    fun render(field: OneofField): String =
        "${ProtobufValueTypeRenderer.render(field.fieldType)} ${field.name} = ${field.index};"

    fun render(value: EnumValue): String = "${value.name} = ${value.index};"

    private fun renderPrefix(cardinality: Cardinality): String = when (cardinality) {
        Cardinality.REQUIRED -> ""
        Cardinality.OPTIONAL -> "optional "
        Cardinality.REPEATED -> "repeated "
    }
}
