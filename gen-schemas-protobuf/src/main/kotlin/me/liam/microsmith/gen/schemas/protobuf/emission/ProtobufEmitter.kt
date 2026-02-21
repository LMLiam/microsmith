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
import me.liam.microsmith.gen.schemas.protobuf.render.renderEnum
import me.liam.microsmith.gen.schemas.protobuf.render.renderMessage
import me.liam.microsmith.gen.schemas.protobuf.render.renderProtobufFile
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
        validateForEmission()

        val contents =
            when (val currentType = schema) {
                is Message ->
                    renderProtobufFile(
                        qualifiedName = qualifiedName,
                        declaration = currentType.renderMessage(),
                        imports = currentType.collectImports(qualifiedName),
                    )

                is Enum ->
                    renderProtobufFile(
                        qualifiedName = qualifiedName,
                        declaration = currentType.renderEnum(),
                    )
            }

        return GeneratedFile(
            relativePath = qualifiedName.relativePath(),
            contents = contents.toByteArray(StandardCharsets.UTF_8),
        )
    }
}
