package io.github.lmliam.microsmith.resolve.schemas.protobuf

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.dsl.schemas.core.SchemasExtension
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Enum
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.github.lmliam.microsmith.resolve.core.DomainResolver
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import kotlin.reflect.KClass

@ServiceProvider(DomainResolver::class)
class ProtobufSchemasResolver : DomainResolver<SchemasExtension, ResolvedProtobufSchemaModel> {
    override val authoringType: KClass<SchemasExtension> = SchemasExtension::class
    override val resolvedType: KClass<ResolvedProtobufSchemaModel> = ResolvedProtobufSchemaModel::class

    override fun resolve(authoring: SchemasExtension): ResolvedProtobufSchemaModel? {
        val schemas =
            authoring.schemas
                .filterIsInstance<ProtobufSchema>()
                .filter { schema ->
                    when (schema.schema) {
                        is Message, is Enum -> true
                        else -> false
                    }
                }
                .sortedBy(ProtobufSchema::name)
                .map { schema ->
                    ResolvedProtobufSchema(
                        schema = schema,
                        qualifiedName = QualifiedSchemaName.parse(schema.name),
                    )
                }
        return schemas.takeIf(List<*>::isNotEmpty)?.let(::ResolvedProtobufSchemaModel)
    }
}
