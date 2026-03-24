package io.github.lmliam.microsmith.dsl.services.core

internal data class ServiceKey(val name: String) {
    init {
        require(name.isNotBlank()) { "Service name cannot be blank." }
    }

    override fun toString() = name

    companion object {
        fun of(service: Service) = ServiceKey(service.name)
    }
}
