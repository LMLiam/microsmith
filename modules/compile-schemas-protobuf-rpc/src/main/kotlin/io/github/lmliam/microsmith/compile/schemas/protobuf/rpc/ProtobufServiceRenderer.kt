package io.github.lmliam.microsmith.compile.schemas.protobuf.rpc

import io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc.ProtobufRpcEndpoint
import io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc.ProtobufRpcServiceArtifact

internal object ProtobufServiceRenderer {
    private const val INDENT = "  "

    fun render(service: ProtobufRpcServiceArtifact): String = buildString {
        appendLine("service ${service.id.serviceName} {")
        service.operations.forEach { operation ->
            append(INDENT)
            append("rpc ${operation.name} (")
            append(render(operation.request))
            append(") returns (")
            append(render(operation.response))
            appendLine(");")
        }
        append("}")
    }

    private fun render(endpoint: ProtobufRpcEndpoint): String = buildString {
        if (endpoint.streaming) {
            append("stream ")
        }
        append(endpoint.typeName)
    }
}
