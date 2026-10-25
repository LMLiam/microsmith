package io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc

import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufDeclarationContext

internal class ServiceBuilder(private val name: String, private val declarationContext: ProtobufDeclarationContext) :
    ServiceScope {
    private val routeNames = mutableSetOf<String>()
    private val rpcs = mutableListOf<Rpc>()

    override fun String.invoke(block: RpcScope.() -> Unit) {
        require(isNotBlank()) { "RPC name cannot be blank." }

        require(routeNames.add(this)) { "Duplicate RPC name: $this" }

        val builder =
            RpcBuilder(
                name = this,
                declarationContext = declarationContext,
            )

        builder.block()
        rpcs += builder.build()
    }

    fun build(): Service =
        Service(
            name = name,
            rpcs = rpcs.toList(),
        )
}
