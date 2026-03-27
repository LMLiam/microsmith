package io.github.lmliam.microsmith.resolve.services.dotnet

import io.github.lmliam.microsmith.dsl.services.dotnet.core.DotnetTarget
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel
import io.github.lmliam.microsmith.dsl.services.dotnet.core.solution.DotnetSolution

/**
 * Resolved per-service .NET generation state.
 */
data class ResolvedDotnetService(
    val name: String,
    val target: DotnetTarget,
    val solution: DotnetSolution,
    val project: String,
    val models: Map<String, DotnetModel>,
)
