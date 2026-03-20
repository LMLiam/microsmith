package io.github.lmliam.microsmith.gen.schemas.protobuf.emission

import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Type
import java.util.ServiceLoader
import kotlin.reflect.KClass

internal class ProtobufDeclarationHandlerRegistry(
    supports: List<ProtobufDeclarationHandler<*>> = loadSupports(),
) {
    private val supportsByType = indexSupports(supports)

    fun resolve(type: Type): ProtobufDeclarationHandler<Type> {
        return supportsByType[type::class]?.cast()
            ?: error("No protobuf declaration support registered for ${type::class.qualifiedName}.")
    }

    private fun indexSupports(
        supports: List<ProtobufDeclarationHandler<*>>,
    ): Map<KClass<out Type>, ProtobufDeclarationHandler<*>> {
        val duplicates = supports.groupBy(ProtobufDeclarationHandler<*>::type).filterValues { it.size > 1 }
        require(duplicates.isEmpty()) {
            val duplicateNames = duplicates.keys.joinToString { it.qualifiedName ?: it.toString() }
            "Duplicate protobuf declaration support registered for: $duplicateNames"
        }

        return supports.associateBy(ProtobufDeclarationHandler<*>::type)
    }

    @Suppress("UNCHECKED_CAST")
    private fun ProtobufDeclarationHandler<*>.cast(): ProtobufDeclarationHandler<Type> {
        return this as ProtobufDeclarationHandler<Type>
    }

    companion object {
        private fun loadSupports(): List<ProtobufDeclarationHandler<*>> {
            return buildList {
                add(MessageDeclarationHandler)
                add(EnumDeclarationHandler)
                addAll(ServiceLoader.load(ProtobufDeclarationHandler::class.java).iterator().asSequence().toList())
            }
        }
    }
}
