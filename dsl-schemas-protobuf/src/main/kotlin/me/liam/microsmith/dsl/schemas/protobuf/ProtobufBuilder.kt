package me.liam.microsmith.dsl.schemas.protobuf

import me.liam.microsmith.dsl.schemas.protobuf.types.EnumBuilder
import me.liam.microsmith.dsl.schemas.protobuf.types.MessageBuilder

class ProtobufBuilder(private val segments: List<String> = emptyList()) : ProtobufScope {
    private val schemasByName = mutableMapOf<String, ProtobufSchema>()
    private val registeredNames = mutableSetOf<String>()

    private fun register(name: String, schema: ProtobufSchema) {
        require(name !in registeredNames) { "Duplicate protobuf schema name: $name" }
        registeredNames += name
        schemasByName[name] = schema
    }

    override fun message(name: String, block: MessageScope.() -> Unit) {
        val fqName = (segments + name).joinToString(".")
        register(
            fqName,
            ProtobufSchema(
                fqName,
                schema = MessageBuilder(name, segments).apply(block).build(),
            ),
        )
    }

    override fun enum(name: String, block: EnumScope.() -> Unit) {
        val fqName = (segments + name).joinToString(".")
        register(
            fqName,
            ProtobufSchema(
                fqName,
                schema = EnumBuilder(name).apply(block).build(),
            ),
        )
    }

    override operator fun String.invoke(block: ProtobufScope.() -> Unit) {
        val namespaceSegments = split('.')
        require(namespaceSegments.none { it.isBlank() }) { "Namespace contains empty segments: '$this'" }
        ProtobufBuilder(segments + namespaceSegments).apply(block).build().forEach { register(it.name, it) }
    }

    override fun version(version: Int, block: ProtobufScope.() -> Unit) {
        require(version > 0) { "Version must be positive, but was $version." }
        ProtobufBuilder(segments + "v$version").apply(block).build().forEach { register(it.name, it) }
    }

    fun build() = schemasByName.values.toSet()
}
