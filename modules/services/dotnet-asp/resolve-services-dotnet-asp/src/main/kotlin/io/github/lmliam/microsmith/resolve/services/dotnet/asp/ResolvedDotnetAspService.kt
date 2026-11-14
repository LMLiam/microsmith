package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModel

data class ResolvedDotnetAspService(
    val name: String,
    val solutionName: String,
    val projectName: String,
    val targetFrameworkMoniker: String,
    val ports: ResolvedDotnetAspPorts,
    val models: Map<String, DotnetModel>,
    val rest: ResolvedDotnetAspRest,
)
