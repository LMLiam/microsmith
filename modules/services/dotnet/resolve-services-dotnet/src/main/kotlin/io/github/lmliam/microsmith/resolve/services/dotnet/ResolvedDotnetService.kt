package io.github.lmliam.microsmith.resolve.services.dotnet

import io.github.lmliam.microsmith.dsl.services.dotnet.DotnetTarget
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModel
import io.github.lmliam.microsmith.dsl.services.dotnet.solution.DotnetSolution

/** Resolved per-service .NET generation state. */
public data class ResolvedDotnetService(
    public val name: String,
    public val target: DotnetTarget,
    public val solution: DotnetSolution,
    public val project: String,
    public val models: Map<String, DotnetModel>,
)
