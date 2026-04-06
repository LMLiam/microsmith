package io.github.lmliam.microsmith.resolve.schemas.core

import io.github.lmliam.microsmith.dsl.schemas.core.Schema
import io.github.lmliam.microsmith.resolve.core.ResolvedModel

/**
 * Finalized schemas model at the domain-root level.
 */
data class ResolvedSchemasModel(val schemas: Set<Schema>) : ResolvedModel
