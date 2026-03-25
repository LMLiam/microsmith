package io.github.lmliam.microsmith.gen.services

import io.github.lmliam.microsmith.dsl.core.ModelExtension
import java.util.ServiceLoader
import kotlin.reflect.KClass

/**
 * Resolves emitters for the extension payloads attached to the services aggregate and individual services.
 */
class ServiceEmitterRegistry(
    emitters: List<ServiceEmitter<*>> = loadServiceEmitters(),
) {
    private val emittersByType: Map<KClass<out ModelExtension>, List<ServiceEmitter<*>>> = indexEmitters(emitters)

    fun resolve(extension: ModelExtension): List<ServiceEmitter<ModelExtension>> {
        return emittersByType[extension::class]
            ?.map { it.cast() }
            ?: error("No emitter found for services-model extension type: ${extension::class}")
    }

    private fun indexEmitters(
        emitters: List<ServiceEmitter<*>>,
    ): Map<KClass<out ModelExtension>, List<ServiceEmitter<*>>> {
        val emittersByType = emitters.groupBy(ServiceEmitter<*>::type)
        emittersByType.forEach { (type, registrations) ->
            val duplicateImplementations =
                registrations
                    .groupBy { it::class }
                    .filterValues { it.size > 1 }
                    .keys
                    .map { it.qualifiedName ?: it.toString() }
                    .sorted()
            require(duplicateImplementations.isEmpty()) {
                "Duplicate service emitters registered for extension type ${formatType(type)}: " +
                    duplicateImplementations.joinToString(", ")
            }
        }

        return emittersByType.mapValues { (_, registrations) ->
            registrations.sortedBy { it::class.qualifiedName ?: it::class.toString() }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun ServiceEmitter<*>.cast(): ServiceEmitter<ModelExtension> = this as ServiceEmitter<ModelExtension>

    private fun formatType(type: KClass<out ModelExtension>): String = type.qualifiedName ?: type.toString()
}

private fun loadServiceEmitters(): List<ServiceEmitter<*>> =
    ServiceLoader.load(ServiceEmitter::class.java).iterator().asSequence().toList()
