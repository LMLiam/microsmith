package io.github.lmliam.microsmith.dsl.services.core

/**
 * Internal builder used within the `services { ... }` DSL block.
 */
class ServicesBuilder : ServicesScope {
    private val servicesByKey = linkedMapOf<ServiceKey, Service>()
    internal val services: Set<Service>
        get() = servicesByKey.values.toSet()

    fun register(service: Service) {
        val serviceKey = ServiceKey.of(service)
        require(serviceKey !in servicesByKey) {
            "Duplicate service registration for '$serviceKey'."
        }

        servicesByKey[serviceKey] = service
    }

    fun toExtension() = ServicesExtension(services)

    override fun String.invoke(block: ServiceScope.() -> Unit) {
        require(isNotBlank()) { "Service name cannot be blank." }
        register(ServiceBuilder(this).apply(block).build())
    }
}
