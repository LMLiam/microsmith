package io.github.lmliam.microsmith.lower.schemas.protobuf.rpc

import io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc.ProtobufRpcServiceArtifact

internal object ProtobufServiceRenderer {
    private const val INDENT = "  "

    fun render(service: ProtobufRpcServiceArtifact): String = buildString {
        appendLine("service ${service.id.serviceName} {")
        service.operations.forEach { operation ->
            append(INDENT)
            append("rpc ${operation.name} (")
            append(render(operation.requestTypeName, operation.requestStreaming))
            append(") returns (")
            append(render(operation.responseTypeName, operation.responseStreaming))
            appendLine(");")
        }
        append("}")
    }

    private fun render(typeName: String, streaming: Boolean): String = buildString {
        if (streaming) {
            append("stream ")
        }
        append(typeName)
    }
}
