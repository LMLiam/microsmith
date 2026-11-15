package io.github.lmliam.microsmith.artifact.schemas.protobuf.render

import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufDeclaration
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnum
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufMessage

private const val INDENT = "  "

internal class ProtobufDeclarationRenderer(
    private val reservedSections: ProtobufReservedSectionRenderer,
    private val fields: ProtobufFieldRenderer,
) {
    fun render(declaration: ResolvedProtobufDeclaration): String =
        when (declaration) {
            is ResolvedProtobufMessage -> render(declaration)
            is ResolvedProtobufEnum -> render(declaration)
        }

    fun render(message: ResolvedProtobufMessage): String = buildString {
        appendLine("message ${message.name} {")

        reservedSections.render(message.reservations)?.let { appendIndentedLine(it) }

        message.fields.forEach { appendIndentedLine(fields.render(it)) }
        message.oneofs.forEach { appendIndentedLine(fields.render(it)) }

        append("}")
    }

    fun render(enum: ResolvedProtobufEnum): String = buildString {
        appendLine("enum ${enum.name} {")

        reservedSections.render(enum.reservations)?.let { appendLine(it.prependIndent(INDENT)) }
        enum.values.forEach { appendLine(fields.render(it).prependIndent(INDENT)) }

        append("}")
    }
}

private fun StringBuilder.appendIndentedLine(value: String) {
    appendLine(value.prependIndent(INDENT))
}
