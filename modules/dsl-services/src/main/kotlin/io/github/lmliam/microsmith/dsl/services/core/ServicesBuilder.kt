package io.github.lmliam.microsmith.dsl.services.core

import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import kotlin.reflect.KClass

/**
 * Internal builder used within the `services { ... }` DSL block.
 */
class ServicesBuilder : ServicesScope {
    private var model = ServicesModel.empty()
    private val servicesByKey = linkedMapOf<String, Service>()
    internal val services: Set<Service>
        get() = servicesByKey.values.toSet()

    fun <T : MicrosmithExtension> put(type: KClass<T>, ext: T) {
        model = model.with(type, ext)
    }

    fun register(service: Service) {
        val serviceKey = service.serviceKey()
        require(serviceKey !in servicesByKey) {
            "Duplicate service registration for '$serviceKey'."
        }

        servicesByKey[serviceKey] = service
    }

    fun toExtension() = ServicesExtension(services, model)

    override fun String.invoke(block: ServiceScope.() -> Unit) {
        register(ServiceBuilder(serviceKey(this)).apply(block).build())
    }
}
