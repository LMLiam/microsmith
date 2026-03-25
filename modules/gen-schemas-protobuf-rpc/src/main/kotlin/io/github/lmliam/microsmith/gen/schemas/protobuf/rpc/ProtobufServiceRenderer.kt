package io.github.lmliam.microsmith.gen.schemas.protobuf.rpc

import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcEndpoint
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcSchema

internal object ProtobufServiceRenderer {
    private const val INDENT = "  "

    fun render(service: ResolvedProtobufRpcSchema): String = buildString {
        appendLine("service ${service.qualifiedName.typeName} {")
        service.rpcs.forEach { rpc ->
            append(INDENT)
            append("rpc ${rpc.name} (")
            append(render(rpc.request))
            append(") returns (")
            append(render(rpc.response))
            appendLine(");")
        }
        append("}")
    }

    private fun render(endpoint: ResolvedProtobufRpcEndpoint): String = buildString {
        if (endpoint.streaming) {
            append("stream ")
        }
        append(endpoint.qualifiedTypeName)
    }
}
