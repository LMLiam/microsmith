package io.github.lmliam.microsmith.gen.services.dotnet

import io.github.lmliam.microsmith.dsl.services.dotnet.core.DotnetTarget
import io.github.lmliam.microsmith.dsl.services.dotnet.core.solution.DotnetSolution

/**
 * Resolved .NET workspace state after DSL normalisation.
 */
data class DotnetWorkspace(
    val target: DotnetTarget?,
    val solutions: Map<String, DotnetSolution>,
    val services: Map<String, ResolvedDotnetService>,
)
