package io.github.lmliam.microsmith.gen.schemas.protobuf.render

import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Cardinality
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.CardinalityField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Field
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.OneofField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ReferenceField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ScalarField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.EnumValue
import io.github.lmliam.microsmith.gen.schemas.protobuf.emission.invalidTopLevelOneofField

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
