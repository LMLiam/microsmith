package io.github.lmliam.microsmith.dsl.services.core

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl

/**
 * Marker interface for a named service block inside `services { ... }`.
 *
 * Feature modules add their own service-scoped entrypoints here
 * (for example, `fun ServiceScope.dotnet { ... }`).
 */
@MicrosmithDsl
interface ServiceScope
