package io.github.lmliam.microsmith.gen.services

import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import java.util.ServiceLoader
import kotlin.reflect.KClass

/**
 * Resolves shared service emitters by concrete extension class and rejects ambiguous registrations.
 */
class SharedServiceEmitterRegistry(
    emitters: List<SharedServiceEmitter<*>> = loadSharedServiceEmitters(),
) {
    private val emittersByType: Map<KClass<out MicrosmithExtension>, SharedServiceEmitter<*>> =
        indexEmitters(emitters)

    fun resolve(extension: MicrosmithExtension): SharedServiceEmitter<MicrosmithExtension> =
        emittersByType[extension::class]
            ?.cast()
            ?: error("No emitter found for shared service extension type: ${extension::class}")

    private fun indexEmitters(
        emitters: List<SharedServiceEmitter<*>>,
    ): Map<KClass<out MicrosmithExtension>, SharedServiceEmitter<*>> {
        val duplicates = emitters.groupBy(SharedServiceEmitter<*>::type).filterValues { it.size > 1 }
        require(duplicates.isEmpty()) {
            val types = duplicates.keys.map(::formatType).sorted().joinToString(", ")
            "Duplicate shared service emitters registered for extension types: $types"
        }

        return emitters.associateBy(SharedServiceEmitter<*>::type)
    }

    @Suppress("UNCHECKED_CAST")
    private fun SharedServiceEmitter<*>.cast(): SharedServiceEmitter<MicrosmithExtension> =
        this as SharedServiceEmitter<MicrosmithExtension>

    private fun formatType(type: KClass<out MicrosmithExtension>): String = type.qualifiedName ?: type.toString()
}

private fun loadSharedServiceEmitters(): List<SharedServiceEmitter<*>> =
    ServiceLoader.load(SharedServiceEmitter::class.java).iterator().asSequence().toList()
