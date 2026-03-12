package me.liam.microsmith.gen.schemas.protobuf.render

import me.liam.microsmith.dsl.schemas.protobuf.types.Enum
import me.liam.microsmith.dsl.schemas.protobuf.types.Message

internal object ProtobufDeclarationRenderer {
    private const val INDENT = "  "

    fun render(message: Message): String = buildString {
        appendLine("message ${message.name} {")
        ProtobufReservedSectionRenderer.render(message)?.let { appendIndentedLine(it) }
        message.fields.forEach { appendIndentedLine(ProtobufFieldRenderer.render(it)) }
        message.oneofs.forEach { appendIndentedLine(ProtobufFieldRenderer.render(it)) }
        append("}")
    }

    fun render(enum: Enum): String = buildString {
        appendLine("enum ${enum.name} {")
        ProtobufReservedSectionRenderer.render(enum)?.let { appendLine(it.prependIndent(INDENT)) }
        enum.values.forEach { appendLine(ProtobufFieldRenderer.render(it).prependIndent(INDENT)) }
        append("}")
    }

    private fun StringBuilder.appendIndentedLine(value: String) {
        appendLine(value.prependIndent(INDENT))
    }
}
