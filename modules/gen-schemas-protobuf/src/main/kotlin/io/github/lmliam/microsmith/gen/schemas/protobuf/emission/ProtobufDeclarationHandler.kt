package io.github.lmliam.microsmith.gen.schemas.protobuf.emission

import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Type
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import kotlin.reflect.KClass

internal interface ProtobufDeclarationHandler<T : Type> {
    val type: KClass<T>

    fun validate(schema: ProtobufSchema, qualifiedName: QualifiedSchemaName)

    fun render(declaration: T): String

    fun collectImports(declaration: T, current: QualifiedSchemaName): List<String> = emptyList()
}
