package io.github.lmliam.microsmith.resolve.services.dotnet.resolution

import io.github.lmliam.microsmith.dsl.services.dotnet.DotnetTarget
import io.github.lmliam.microsmith.dsl.services.dotnet.solution.DotnetSolution
import io.github.lmliam.microsmith.resolve.ResolvedModel
import io.github.lmliam.microsmith.resolve.services.dotnet.ResolvedDotnetService

/**
 * Resolved .NET workspace state after DSL normalisation.
 */
data class DotnetWorkspace(
    val target: DotnetTarget?,
    val solutions: Map<String, DotnetSolution>,
    val services: Map<String, ResolvedDotnetService>,
) : ResolvedModel
