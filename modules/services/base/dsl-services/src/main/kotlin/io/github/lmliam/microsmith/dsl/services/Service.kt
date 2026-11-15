package io.github.lmliam.microsmith.dsl.services

/** Immutable service declaration produced by the core service DSL. */
public data class Service(public val name: String, public val model: ServiceModel) {
    init {
        serviceKey(name)
    }
}

internal fun serviceKey(name: String): String {
    require(name.isNotBlank()) { "Service name cannot be blank." }
    return name
}

internal fun Service.serviceKey(): String = serviceKey(name)
