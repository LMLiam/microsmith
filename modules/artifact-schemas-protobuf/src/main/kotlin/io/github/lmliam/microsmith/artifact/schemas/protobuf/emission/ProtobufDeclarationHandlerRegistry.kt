package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission

import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Type
import kotlin.reflect.KClass

internal class ProtobufDeclarationHandlerRegistry(
    handlers: List<ProtobufDeclarationHandler<*>> = defaultHandlers,
) {
    private val handlersByType = indexHandlers(handlers)

    fun resolve(type: Type): ProtobufDeclarationHandler<Type> {
        return handlersByType[type::class]?.cast()
            ?: error("No protobuf declaration support registered for ${type::class.qualifiedName}.")
    }

    private fun indexHandlers(
        handlers: List<ProtobufDeclarationHandler<*>>,
    ): Map<KClass<out Type>, ProtobufDeclarationHandler<*>> {
        val duplicates = handlers.groupBy(ProtobufDeclarationHandler<*>::type).filterValues { it.size > 1 }
        require(duplicates.isEmpty()) {
            val duplicateNames = duplicates.keys.joinToString { it.qualifiedName ?: it.toString() }
            "Duplicate protobuf declaration support registered for: $duplicateNames"
        }

        return handlers.associateBy(ProtobufDeclarationHandler<*>::type)
    }

    @Suppress("UNCHECKED_CAST")
    private fun ProtobufDeclarationHandler<*>.cast(): ProtobufDeclarationHandler<Type> {
        return this as ProtobufDeclarationHandler<Type>
    }

    private companion object {
        val defaultHandlers = listOf(MessageDeclarationHandler, EnumDeclarationHandler)
    }
}
