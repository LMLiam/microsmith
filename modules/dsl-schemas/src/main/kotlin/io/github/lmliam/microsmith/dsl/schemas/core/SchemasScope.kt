package io.github.lmliam.microsmith.dsl.schemas.core

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl

/**
 * Marker interface for the `schemas { ... }` DSL block.
 *
 * This scope is intentionally empty: dialect modules contribute
 * extension functions on [SchemasScope], such as `protobuf { ... }`.
 */
@MicrosmithDsl
interface SchemasScope
