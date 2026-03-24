package io.github.lmliam.microsmith.gen.services.dotnet

import io.github.lmliam.microsmith.dsl.services.dotnet.core.DotnetModel
import io.github.lmliam.microsmith.dsl.services.dotnet.core.DotnetSolution
import io.github.lmliam.microsmith.dsl.services.dotnet.core.DotnetTarget

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
