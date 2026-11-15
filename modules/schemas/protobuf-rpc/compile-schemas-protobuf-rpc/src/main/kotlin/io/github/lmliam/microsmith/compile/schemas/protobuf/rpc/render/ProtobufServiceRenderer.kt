package io.github.lmliam.microsmith.compile.schemas.protobuf.rpc.render

import dev.zacsweers.metro.Inject
import io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc.ProtobufRpcEndpoint
import io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc.ProtobufRpcServiceArtifact

private const val INDENT = "  "

@Inject
internal class ProtobufServiceRenderer {
    fun render(service: ProtobufRpcServiceArtifact): String = buildString {
        if (service.operations.isEmpty()) {
            append("service ${service.id.serviceName} {}")
            return@buildString
        }
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
