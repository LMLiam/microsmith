package io.github.lmliam.microsmith.resolve.services.core

import io.github.lmliam.microsmith.dsl.services.core.Service
import io.github.lmliam.microsmith.resolve.core.ResolvedModel

/**
 * Finalized services model at the domain-root level.
 */
data class ResolvedServicesModel(
    val services: Set<Service>,
) : ResolvedModel
