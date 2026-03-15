package io.github.lmliam.microsmith.gen.schemas.protobuf.rpc

import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.RpcEndpoint
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.Service

internal object ProtobufServiceRenderer {
    private const val INDENT = "  "

    fun render(service: Service): String = buildString {
        appendLine("service ${service.name} {")
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

    private fun render(endpoint: RpcEndpoint): String = buildString {
        if (endpoint.streaming) {
            append("stream ")
        }
        append(endpoint.reference.name)
    }
}
