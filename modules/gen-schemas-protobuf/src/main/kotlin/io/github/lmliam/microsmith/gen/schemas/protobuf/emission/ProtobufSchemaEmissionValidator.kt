package io.github.lmliam.microsmith.gen.schemas.protobuf.emission

import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.gen.schemas.protobuf.names.QualifiedSchemaName

internal object ProtobufSchemaEmissionValidator {
    private val declarationSupportRegistry = ProtobufDeclarationSupportRegistry()

    fun validate(schema: ProtobufSchema) {
        val qualifiedName = QualifiedSchemaName.parse(schema.name)
        declarationSupportRegistry.resolve(schema.schema).validate(schema, qualifiedName)
    }
}
