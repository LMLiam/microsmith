package io.github.lmliam.microsmith.dsl.schemas.protobuf

import io.github.lmliam.microsmith.dsl.schemas.protobuf.support.getReferencePath
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.EnumBuilder
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.MessageBuilder
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Type

internal class ProtobufBuilder(private val segments: List<String> = emptyList()) : ProtobufDeclarationContext {
    private val schemasByName = mutableMapOf<String, ProtobufSchema>()
    private val registeredNames = mutableSetOf<String>()

    private fun register(name: String, schema: ProtobufSchema) {
        require(name !in registeredNames) { "Duplicate protobuf schema name: $name" }
        registeredNames += name
        schemasByName[name] = schema
    }

    override fun message(name: String, block: MessageScope.() -> Unit) {
        registerDeclaration(name, MessageBuilder(name, segments).apply(block).build())
    }

    override fun enum(name: String, block: EnumScope.() -> Unit) {
        registerDeclaration(name, EnumBuilder(name).apply(block).build())
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

    override fun qualifyName(name: String): String = (segments + name).joinToString(".")

    override fun resolveReference(target: String): String = getReferencePath(segments, target).joinToString(".")

    override fun registerDeclaration(name: String, declaration: Type) {
        val fqName = qualifyName(name)
        register(fqName, ProtobufSchema(fqName, declaration))
    }

    fun build() = schemasByName.values.toSet()
}
