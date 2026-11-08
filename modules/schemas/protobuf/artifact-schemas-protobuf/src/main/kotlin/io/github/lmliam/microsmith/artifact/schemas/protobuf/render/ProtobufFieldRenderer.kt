package io.github.lmliam.microsmith.artifact.schemas.protobuf.render

import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Cardinality
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnumValue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufField
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufOneof

internal object ProtobufFieldRenderer {
    fun render(field: ResolvedProtobufField): String =
        when (field) {
            is ResolvedProtobufField.Scalar ->
                renderCardinalityField(
                    field.cardinality,
                    ProtobufValueTypeRenderer.render(field.type),
                    field.name.value,
                    field.number.value,
                )

            is ResolvedProtobufField.Reference ->
                renderCardinalityField(
                    field.cardinality,
                    field.reference.target.fullyQualifiedName,
                    field.name.value,
                    field.number.value,
                )

            is ResolvedProtobufField.Map ->
                buildString {
                    append("map<")
                    append(ProtobufValueTypeRenderer.render(field.key))
                    append(", ")
                    append(ProtobufValueTypeRenderer.render(field.value))
                    append("> ")
                    append(field.name.value)
                    append(" = ")
                    append(field.number.value)
                    append(";")
                }
        }

    fun render(oneof: ResolvedProtobufOneof): String = buildString {
        appendLine("oneof ${oneof.name} {")

        oneof.fields.forEach { appendLine(render(it).prependIndent("  ")) }

        append("}")
    }

    fun render(field: ResolvedProtobufOneof.Field): String =
        "${ProtobufValueTypeRenderer.render(field.type)} ${field.name} = ${field.number.value};"

    fun render(value: ResolvedProtobufEnumValue): String = "${value.name} = ${value.number.value};"

    private fun renderCardinalityField(cardinality: Cardinality, type: String, name: String, number: Int): String =
        buildString {
            append(
                when (cardinality) {
                    Cardinality.SINGULAR -> ""
                    Cardinality.OPTIONAL -> "optional "
                    Cardinality.REPEATED -> "repeated "
                }
            )

            append(type)
            append(' ')
            append(name)
            append(" = ")
            append(number)
            append(";")
        }
}
