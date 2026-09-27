package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Enum
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

internal class ProtobufSymbolTable(schemas: Collection<ProtobufSchema>) {
    data class Symbol(val identity: QualifiedSchemaName, val kind: ProtobufDeclarationKind)

    private val symbols = schemas
        .mapNotNull { schema ->
            val kind = when (schema.schema) {
                is Message -> ProtobufDeclarationKind.MESSAGE
                is Enum -> ProtobufDeclarationKind.ENUM
                else -> null
            }

            kind?.let {
                val identity = QualifiedSchemaName.parse(schema.name)
                Symbol(identity, kind)
            }
        }
        .associateBy { it.identity.fullyQualifiedName }

    fun find(identity: QualifiedSchemaName): Symbol? = symbols[identity.fullyQualifiedName]
}
