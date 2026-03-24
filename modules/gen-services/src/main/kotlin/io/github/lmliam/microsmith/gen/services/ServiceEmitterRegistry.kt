package io.github.lmliam.microsmith.gen.services

import io.github.lmliam.microsmith.dsl.services.core.ServiceExtension
import java.util.ServiceLoader
import kotlin.reflect.KClass

/**
 * Resolves service emitters by concrete service extension class and rejects ambiguous registrations.
 */
internal class ServiceEmitterRegistry(
    emitters: List<ServiceEmitter<*>> = loadServiceEmitters(),
) {
    private val emittersByType: Map<KClass<out ServiceExtension>, ServiceEmitter<*>> = indexEmitters(emitters)

    fun resolve(extension: ServiceExtension): ServiceEmitter<ServiceExtension> = emittersByType[extension::class]
        ?.cast()
        ?: error("No emitter found for service extension type: ${extension::class}")

    private fun indexEmitters(emitters: List<ServiceEmitter<*>>): Map<KClass<out ServiceExtension>, ServiceEmitter<*>> {
        val duplicates = emitters.groupBy(ServiceEmitter<*>::type).filterValues { it.size > 1 }
        require(duplicates.isEmpty()) {
            val types = duplicates.keys.map(::formatType).sorted().joinToString(", ")
            "Duplicate service emitters registered for extension types: $types"
        }

        return emitters.associateBy(ServiceEmitter<*>::type)
    }

    @Suppress("UNCHECKED_CAST")
    private fun ServiceEmitter<*>.cast(): ServiceEmitter<ServiceExtension> = this as ServiceEmitter<ServiceExtension>

    private fun formatType(type: KClass<out ServiceExtension>): String = type.qualifiedName ?: type.toString()
}

private fun loadServiceEmitters(): List<ServiceEmitter<*>> =
    ServiceLoader.load(ServiceEmitter::class.java).iterator().asSequence().toList()
