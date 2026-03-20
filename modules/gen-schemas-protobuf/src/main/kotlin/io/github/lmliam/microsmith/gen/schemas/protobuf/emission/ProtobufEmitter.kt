package io.github.lmliam.microsmith.gen.schemas.protobuf.emission

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.gen.files.FileSpace
import io.github.lmliam.microsmith.gen.files.GeneratedFile
import io.github.lmliam.microsmith.gen.schemas.SchemaEmitter
import io.github.lmliam.microsmith.gen.schemas.protobuf.names.QualifiedSchemaName
import io.github.lmliam.microsmith.gen.schemas.protobuf.render.ProtobufFileRenderer
import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass

/**
 * Emits [ProtobufSchema] instances into `.proto` files under the `proto/` output tree.
 *
 * This emitter is stateless and therefore safe for concurrent use.
 */
@ServiceProvider(SchemaEmitter::class)
class ProtobufEmitter(override val type: KClass<ProtobufSchema> = ProtobufSchema::class) :
    SchemaEmitter<ProtobufSchema> {
    private val declarationSupportRegistry = ProtobufDeclarationHandlerRegistry()

    /**
     * Converts the receiving schema into a deterministic protobuf source file.
     *
     * Output path and file contents are stable for equivalent schema inputs.
     */
    override suspend fun ProtobufSchema.emit(space: FileSpace): GeneratedFile {
        val qualifiedName = QualifiedSchemaName.parse(name)
        val declarationSupport = declarationSupportRegistry.resolve(schema)
        declarationSupport.validate(this, qualifiedName)

        val contents =
            ProtobufFileRenderer.render(
                qualifiedName = qualifiedName,
                declaration = declarationSupport.render(schema),
                imports = declarationSupport.collectImports(schema, qualifiedName),
            )

        return GeneratedFile(
            relativePath = qualifiedName.relativePath(),
            contents = contents.toByteArray(StandardCharsets.UTF_8),
        )
    }
}
