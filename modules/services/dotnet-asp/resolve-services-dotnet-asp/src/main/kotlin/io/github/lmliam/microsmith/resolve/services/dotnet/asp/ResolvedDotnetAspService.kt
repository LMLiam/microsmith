package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModel

public data class ResolvedDotnetAspService(
    public val name: String,
    public val solutionName: String,
    public val projectName: String,
    public val targetFrameworkMoniker: String,
    public val ports: ResolvedDotnetAspPorts,
    public val models: Map<String, DotnetModel>,
    public val rest: ResolvedDotnetAspRest,
)
