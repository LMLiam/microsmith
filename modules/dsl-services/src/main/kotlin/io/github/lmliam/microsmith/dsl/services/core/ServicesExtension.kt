package io.github.lmliam.microsmith.dsl.services.core

import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import kotlin.reflect.KClass

/**
 * Root extension that holds all declared services and shared services-scoped extensions.
 */
data class ServicesExtension(val services: Set<Service>, val model: ServicesModel = ServicesModel.empty()) :
    MicrosmithExtension {
    init {
        val duplicateKeys =
            services
                .groupBy(Service::serviceKey)
                .filterValues { it.size > 1 }
                .keys
                .sorted()

        require(duplicateKeys.isEmpty()) {
            "ServicesExtension contains duplicate service keys: ${duplicateKeys.joinToString(", ")}"
        }
    }

    private val index = services.associateBy(Service::serviceKey)

    fun find(name: String) = index[serviceKey(name)]

    fun require(name: String): Service {
        val serviceKey = serviceKey(name)
        return index[serviceKey] ?: error("Service not found: $serviceKey")
    }

    fun all() = services

    @Suppress("UNCHECKED_CAST")
    fun <T : MicrosmithExtension> get(type: KClass<T>) = model.get(type) as? T?

    inline fun <reified T : MicrosmithExtension> get(): T? = get(T::class)

    internal fun <T : MicrosmithExtension> with(type: KClass<T>, value: T) = copy(
        model = model.with(type, value),
    )

    fun merge(other: ServicesExtension): ServicesExtension {
        val existingKeys = services.mapTo(mutableSetOf(), Service::serviceKey)
        val collisions =
            other.services
                .map(Service::serviceKey)
                .filter { it in existingKeys }
                .distinct()
                .sorted()

        require(collisions.isEmpty()) {
            "Duplicate service keys while merging ServicesExtension: ${collisions.joinToString(", ")}"
        }

        return copy(
            services = services + other.services,
            model = model.merge(other.model),
        )
    }
}
