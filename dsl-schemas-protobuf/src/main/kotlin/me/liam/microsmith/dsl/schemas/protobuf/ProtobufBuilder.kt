package me.liam.microsmith.dsl.schemas.protobuf

import me.liam.microsmith.dsl.schemas.protobuf.types.EnumBuilder
import me.liam.microsmith.dsl.schemas.protobuf.types.MessageBuilder

class ProtobufBuilder(
    private val segments: List<String> = emptyList()
) : ProtobufScope {
    private val schemas = mutableSetOf<ProtobufSchema>()

    override fun message(
        name: String,
        block: MessageScope.() -> Unit
    ) {
        schemas +=
            ProtobufSchema(
                (segments + name).joinToString("."),
                schema = MessageBuilder(name, segments).apply(block).build()
            )
    }

    override fun enum(
        name: String,
        block: EnumScope.() -> Unit
    ) {
        schemas +=
            ProtobufSchema(
                (segments + name).joinToString("."),
                schema = EnumBuilder(name).apply(block).build()
            )
    }

    override operator fun String.invoke(block: ProtobufScope.() -> Unit) {
        ProtobufBuilder(segments + this.split('.')).apply(block).schemas.forEach { schemas += it }
    }

    override fun version(
        version: Int,
        block: ProtobufScope.() -> Unit
    ) {
        ProtobufBuilder(segments + "v$version").apply(block).schemas.forEach { schemas += it }
    }

    fun build() = schemas.toSet()
}