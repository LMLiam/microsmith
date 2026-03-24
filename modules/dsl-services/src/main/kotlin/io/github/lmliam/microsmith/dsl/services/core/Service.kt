package io.github.lmliam.microsmith.dsl.services.core

/**
 * Immutable service declaration produced by the core service DSL.
 */
data class Service(
    val name: String,
    val model: ServiceModel,
) {
    init {
        require(name.isNotBlank()) { "Service name cannot be blank." }
    }
}
