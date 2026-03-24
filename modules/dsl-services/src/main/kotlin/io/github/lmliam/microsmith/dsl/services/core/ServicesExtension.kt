package io.github.lmliam.microsmith.dsl.services.core

import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension

/**
 * Root extension that holds all declared services.
 */
data class ServicesExtension(val services: Set<Service>) : MicrosmithExtension {
    init {
        val duplicateKeys =
            services
                .groupBy(ServiceKey::of)
                .filterValues { it.size > 1 }
                .keys
                .map(ServiceKey::toString)
                .sorted()

        require(duplicateKeys.isEmpty()) {
            "ServicesExtension contains duplicate service keys: ${duplicateKeys.joinToString(", ")}"
        }
    }

    private val index = services.associateBy(ServiceKey::of)

    fun find(name: String) = index[ServiceKey(name)]

    fun require(name: String): Service {
        return find(name) ?: error("Service not found: ${ServiceKey(name)}")
    }

    fun all() = services

    fun merge(other: ServicesExtension): ServicesExtension {
        val existingKeys = services.mapTo(mutableSetOf(), ServiceKey::of)
        val collisions =
            other.services
                .map(ServiceKey::of)
                .filter { it in existingKeys }
                .map(ServiceKey::toString)
                .distinct()
                .sorted()

        require(collisions.isEmpty()) {
            "Duplicate service keys while merging ServicesExtension: ${collisions.joinToString(", ")}"
        }

        return copy(services = services + other.services)
    }
}
