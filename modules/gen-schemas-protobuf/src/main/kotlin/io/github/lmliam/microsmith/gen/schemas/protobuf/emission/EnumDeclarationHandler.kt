package io.github.lmliam.microsmith.gen.schemas.protobuf.emission

import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Enum
import io.github.lmliam.microsmith.gen.schemas.protobuf.render.ProtobufDeclarationRenderer
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import kotlin.reflect.KClass

internal object EnumDeclarationHandler : ProtobufDeclarationHandler<Enum> {
    override val type: KClass<Enum> = Enum::class

    override fun validate(schema: ProtobufSchema, qualifiedName: QualifiedSchemaName) {
        require(qualifiedName.typeName == schema.schema.name) {
            "Schema name '${schema.name}' must match declaration name '${schema.schema.name}'."
        }
        EnumEmissionValidator.validate(schema.schema as Enum)
    }

    override fun render(declaration: Enum): String = ProtobufDeclarationRenderer.render(declaration)
}
