package io.github.lmliam.microsmith.artifact.schemas.protobuf.render
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnum
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufMessage

private const val INDENT = "  "

internal fun renderDeclaration(message: ResolvedProtobufMessage): String = buildString {
    appendLine("message ${message.name} {")

    ProtobufReservedSectionRenderer.render(message.reservations)?.let { appendIndentedLine(it) }

    message.fields.forEach { appendIndentedLine(ProtobufFieldRenderer.render(it)) }
    message.oneofs.forEach { appendIndentedLine(ProtobufFieldRenderer.render(it)) }

    append("}")
}

internal fun renderDeclaration(enum: ResolvedProtobufEnum): String = buildString {
    appendLine("enum ${enum.name} {")

    ProtobufReservedSectionRenderer.render(enum.reservations)?.let { appendLine(it.prependIndent(INDENT)) }
    enum.values.forEach { appendLine(ProtobufFieldRenderer.render(it).prependIndent(INDENT)) }

    append("}")
}

private fun StringBuilder.appendIndentedLine(value: String) {
    appendLine(value.prependIndent(INDENT))
}
