package io.github.lmliam.microsmith.resolve.services
import io.github.lmliam.microsmith.dsl.services.Service
import io.github.lmliam.microsmith.resolve.ResolvedModel

/**
 * Finalized services model at the domain-root level.
 */
data class ResolvedServicesModel(val services: Set<Service>) : ResolvedModel
