package io.github.lmliam.microsmith.resolve.services.dotnet

import io.github.lmliam.microsmith.dsl.services.dotnet.core.DotnetTarget
import io.github.lmliam.microsmith.dsl.services.dotnet.core.solution.DotnetSolution
import io.github.lmliam.microsmith.resolve.core.ResolvedModel

/**
 * Resolved .NET workspace state after DSL normalisation.
 */
data class DotnetWorkspace(
    val target: DotnetTarget?,
    val solutions: Map<String, DotnetSolution>,
    val services: Map<String, ResolvedDotnetService>,
) : ResolvedModel
