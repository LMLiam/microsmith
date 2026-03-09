package me.liam.microsmith.gen.schemas.protobuf.emission

import me.liam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import me.liam.microsmith.dsl.schemas.protobuf.types.Enum
import me.liam.microsmith.dsl.schemas.protobuf.types.Message
import me.liam.microsmith.gen.schemas.protobuf.names.QualifiedSchemaName

internal object ProtobufSchemaEmissionValidator {
    fun validate(schema: ProtobufSchema) {
        val qualifiedName = QualifiedSchemaName.parse(schema.name)
        require(qualifiedName.typeName == schema.schema.name) {
            "Schema name '${schema.name}' must match declaration name '${schema.schema.name}'."
        }

        when (val currentType = schema.schema) {
            is Message -> MessageEmissionValidator.validate(currentType)
            is Enum -> EnumEmissionValidator.validate(currentType)
        }
    }
}
