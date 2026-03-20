package io.github.lmliam.microsmith.gen.schemas.protobuf.rpc

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.Service
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.github.lmliam.microsmith.gen.schemas.protobuf.emission.ProtobufDeclarationHandler
import io.github.lmliam.microsmith.gen.schemas.protobuf.names.ProtobufNameValidation
import io.github.lmliam.microsmith.gen.schemas.protobuf.names.QualifiedSchemaName
import kotlin.reflect.KClass

@ServiceProvider(ProtobufDeclarationHandler::class)
class ServiceDeclarationHandler : ProtobufDeclarationHandler<Service> {
    override val type: KClass<Service> = Service::class

    override fun validate(schema: ProtobufSchema, qualifiedName: QualifiedSchemaName) {
        require(qualifiedName.typeName == schema.schema.name) {
            "Schema name '${schema.name}' must match declaration name '${schema.schema.name}'."
        }

        val service = schema.schema as Service
        ProtobufNameValidation.requireIdentifier(service.name, "Service name")
        val rpcNames = mutableSetOf<String>()
        service.rpcs.forEach { rpc ->
            ProtobufNameValidation.requireIdentifier(rpc.name, "RPC name")
            require(rpcNames.add(rpc.name)) {
                "Duplicate RPC name in service '${service.name}': ${rpc.name}"
            }
            require(rpc.request.reference.type is Message) {
                "RPC '${rpc.name}' request must target a protobuf message, but was '${rpc.request.reference.name}'."
            }
            require(rpc.response.reference.type is Message) {
                "RPC '${rpc.name}' response must target a protobuf message, but was '${rpc.response.reference.name}'."
            }
        }
    }

    override fun render(declaration: Service): String = ProtobufServiceRenderer.render(declaration)

    override fun collectImports(declaration: Service, current: QualifiedSchemaName): List<String> =
        declaration.collectImports(current)
}
