package io.github.lmliam.microsmith.resolve.schemas

import io.github.lmliam.microsmith.dsl.schemas.Schema
import io.github.lmliam.microsmith.resolve.ResolvedModel

/** Finalized schemas model at the domain-root level. */
data class ResolvedSchemasModel(val schemas: Set<Schema>) : ResolvedModel
