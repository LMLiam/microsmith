package me.liam.microsmith.dsl.schemas.protobuf

import me.liam.microsmith.dsl.schemas.protobuf.enum.EnumBuilder
import me.liam.microsmith.dsl.schemas.protobuf.message.MessageBuilder

class ProtobufBuilder(
    private val segments: List<String> = emptyList()
) : ProtobufScope {
    private val schemas = mutableSetOf<ProtobufSchema>()

    override fun message(name: String, block: MessageScope.() -> Unit) {
        schemas += ProtobufMessageSchema((segments + name).joinToString("/"), message = MessageBuilder(name).apply(block).build())
    }

    override fun enum(name: String, block: EnumScope.() -> Unit) {
        schemas += ProtobufEnumSchema((segments + name).joinToString("/"), enum = EnumBuilder(name).apply(block).build())
    }

    override operator fun String.invoke(block: ProtobufScope.() -> Unit) {
        ProtobufBuilder(segments + this.split('.')).apply(block).schemas.forEach { schemas += it }
    }

    override operator fun Int.invoke(block: ProtobufScope.() -> Unit) {
        version(this, block)
    }

    override fun version(version: Int, block: ProtobufScope.() -> Unit) {
        ProtobufBuilder(segments + "v$version").apply(block).schemas.forEach { schemas += it }
    }

    fun build() = schemas.toSet()
}