package io.github.lmliam.microsmith.gen.schemas.protobuf.emission

import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Type
import java.util.ServiceLoader
import kotlin.reflect.KClass

internal class ProtobufDeclarationSupportRegistry(
    supports: List<ProtobufDeclarationSupport<*>> = loadSupports(),
) {
    private val supportsByType = indexSupports(supports)

    fun resolve(type: Type): ProtobufDeclarationSupport<Type> {
        return supportsByType[type::class]?.cast()
            ?: error("No protobuf declaration support registered for ${type::class.qualifiedName}.")
    }

    private fun indexSupports(
        supports: List<ProtobufDeclarationSupport<*>>,
    ): Map<KClass<out Type>, ProtobufDeclarationSupport<*>> {
        val duplicates = supports.groupBy(ProtobufDeclarationSupport<*>::type).filterValues { it.size > 1 }
        require(duplicates.isEmpty()) {
            val duplicateNames = duplicates.keys.joinToString { it.qualifiedName ?: it.toString() }
            "Duplicate protobuf declaration support registered for: $duplicateNames"
        }

        return supports.associateBy(ProtobufDeclarationSupport<*>::type)
    }

    @Suppress("UNCHECKED_CAST")
    private fun ProtobufDeclarationSupport<*>.cast(): ProtobufDeclarationSupport<Type> {
        return this as ProtobufDeclarationSupport<Type>
    }

    companion object {
        private fun loadSupports(): List<ProtobufDeclarationSupport<*>> {
            return buildList {
                add(MessageDeclarationSupport)
                add(EnumDeclarationSupport)
                addAll(ServiceLoader.load(ProtobufDeclarationSupport::class.java).iterator().asSequence().toList())
            }
        }
    }
}
