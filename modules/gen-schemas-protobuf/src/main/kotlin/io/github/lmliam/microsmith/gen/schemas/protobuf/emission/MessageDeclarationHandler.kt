package io.github.lmliam.microsmith.gen.schemas.protobuf.emission

import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.github.lmliam.microsmith.gen.schemas.protobuf.imports.collectImports
import io.github.lmliam.microsmith.gen.schemas.protobuf.names.QualifiedSchemaName
import io.github.lmliam.microsmith.gen.schemas.protobuf.render.ProtobufDeclarationRenderer
import kotlin.reflect.KClass

internal object MessageDeclarationHandler : ProtobufDeclarationHandler<Message> {
    override val type: KClass<Message> = Message::class

    override fun validate(schema: ProtobufSchema, qualifiedName: QualifiedSchemaName) {
        require(qualifiedName.typeName == schema.schema.name) {
            "Schema name '${schema.name}' must match declaration name '${schema.schema.name}'."
        }
        MessageEmissionValidator.validate(schema.schema as Message)
    }

    override fun render(declaration: Message): String = ProtobufDeclarationRenderer.render(declaration)

    override fun collectImports(declaration: Message, current: QualifiedSchemaName): List<String> =
        declaration.collectImports(current)
}
