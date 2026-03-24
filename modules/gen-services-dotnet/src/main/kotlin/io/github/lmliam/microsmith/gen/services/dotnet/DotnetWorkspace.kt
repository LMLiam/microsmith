package io.github.lmliam.microsmith.gen.services.dotnet

import io.github.lmliam.microsmith.dsl.services.dotnet.core.DotnetSolution
import io.github.lmliam.microsmith.dsl.services.dotnet.core.DotnetTarget

/**
 * Resolved .NET workspace state after DSL normalisation.
 */
data class DotnetWorkspace(
    val target: DotnetTarget?,
    val solutions: Map<String, DotnetSolution>,
    val services: Map<String, ResolvedDotnetService>,
)
