package io.github.lmliam.microsmith.dsl.services

import io.github.lmliam.microsmith.dsl.MergeableExtension
import io.github.lmliam.microsmith.dsl.MicrosmithExtension
import kotlin.reflect.KClass

/** Root extension that holds all declared services and shared services-scoped extensions. */
public data class ServicesExtension(
    public val services: Set<Service>,
    public val model: ServicesModel = ServicesModel.empty(),
) : MicrosmithExtension, MergeableExtension<ServicesExtension> {
    init {
        val duplicateKeys = services.groupBy(Service::serviceKey).filterValues { it.size > 1 }.keys.sorted()

        require(duplicateKeys.isEmpty()) {
            "ServicesExtension contains duplicate service keys: ${duplicateKeys.joinToString(", ")}"
        }
    }

    private val index = services.associateBy(Service::serviceKey)

    internal fun find(name: String): Service? = index[serviceKey(name)]

    public fun require(name: String): Service {
        val serviceKey = serviceKey(name)
        return index[serviceKey] ?: error("Service not found: $serviceKey")
    }

    internal fun all(): Set<Service> = services

    public fun <T : MicrosmithExtension> get(type: KClass<T>): T? = model.get(type)

    public inline fun <reified T : MicrosmithExtension> get(): T? = get(T::class)

    internal fun <T : MicrosmithExtension> with(type: KClass<T>, value: T) = copy(model = model.with(type, value))

    override fun merge(other: ServicesExtension): ServicesExtension {
        val existingKeys = services.mapTo(mutableSetOf(), Service::serviceKey)
        val collisions = other.services.map(Service::serviceKey).filter { it in existingKeys }.distinct().sorted()

        require(collisions.isEmpty()) {
            "Duplicate service keys while merging ServicesExtension: ${collisions.joinToString(", ")}"
        }

        return copy(
            services = services + other.services,
            model = model.merge(other.model),
        )
    }
}
