package io.github.lmliam.microsmith.dsl.services
/**
 * Immutable service declaration produced by the core service DSL.
 */
data class Service(val name: String, val model: ServiceModel) {
    init {
        serviceKey(name)
    }
}

internal fun serviceKey(name: String): String {
    require(name.isNotBlank()) { "Service name cannot be blank." }
    return name
}

internal fun Service.serviceKey(): String = serviceKey(name)
