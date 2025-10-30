package me.liam.microsmith.gen.schemas.protobuf

import com.github.eventhorizonlab.spi.ServiceProvider
import me.liam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import me.liam.microsmith.gen.files.FileSpace
import me.liam.microsmith.gen.files.GeneratedFile
import me.liam.microsmith.gen.schemas.SchemaEmitter
import kotlin.reflect.KClass

@ServiceProvider(SchemaEmitter::class)
class ProtobufEmitter(
    override val type: KClass<ProtobufSchema> = ProtobufSchema::class
) : SchemaEmitter<ProtobufSchema> {
    override suspend fun ProtobufSchema.emit(space: FileSpace): GeneratedFile {
        TODO("Not yet implemented")
    }
}