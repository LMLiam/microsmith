package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission

import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

internal object ProtobufSchemaEmissionValidator {
    private val declarationSupportRegistry = ProtobufDeclarationHandlerRegistry()

    fun validate(schema: ProtobufSchema) {
        val qualifiedName = QualifiedSchemaName.parse(schema.name)
        declarationSupportRegistry.resolve(schema.schema).validate(schema, qualifiedName)
    }
}
