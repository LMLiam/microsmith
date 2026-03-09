package me.liam.microsmith.gen.schemas.protobuf.emission

import com.github.eventhorizonlab.spi.ServiceProvider
import me.liam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import me.liam.microsmith.dsl.schemas.protobuf.types.Enum
import me.liam.microsmith.dsl.schemas.protobuf.types.Message
import me.liam.microsmith.gen.files.FileSpace
import me.liam.microsmith.gen.files.GeneratedFile
import me.liam.microsmith.gen.schemas.SchemaEmitter
import me.liam.microsmith.gen.schemas.protobuf.imports.collectImports
import me.liam.microsmith.gen.schemas.protobuf.names.QualifiedSchemaName
import me.liam.microsmith.gen.schemas.protobuf.render.ProtobufDeclarationRenderer
import me.liam.microsmith.gen.schemas.protobuf.render.ProtobufFileRenderer
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
    /**
     * Converts the receiving schema into a deterministic protobuf source file.
     *
     * Output path and file contents are stable for equivalent schema inputs.
     */
    override suspend fun ProtobufSchema.emit(space: FileSpace): GeneratedFile {
        val qualifiedName = QualifiedSchemaName.parse(name)
        ProtobufSchemaEmissionValidator.validate(this)

        val contents =
            ProtobufFileRenderer.render(
                qualifiedName = qualifiedName,
                declaration = renderDeclaration(),
                imports = collectImports(qualifiedName),
            )

        return GeneratedFile(
            relativePath = qualifiedName.relativePath(),
            contents = contents.toByteArray(StandardCharsets.UTF_8),
        )
    }

    private fun ProtobufSchema.renderDeclaration(): String = when (val currentType = schema) {
        is Message -> ProtobufDeclarationRenderer.render(currentType)
        is Enum -> ProtobufDeclarationRenderer.render(currentType)
    }

    private fun ProtobufSchema.collectImports(qualifiedName: QualifiedSchemaName): List<String> =
        when (val currentType = schema) {
            is Message -> currentType.collectImports(qualifiedName)
            is Enum -> emptyList()
        }
}
