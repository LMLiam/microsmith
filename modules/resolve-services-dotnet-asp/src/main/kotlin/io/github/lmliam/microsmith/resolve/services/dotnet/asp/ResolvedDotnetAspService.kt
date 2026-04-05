package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel
import java.nio.file.Path

data class ResolvedDotnetAspService(
    val name: String,
    val solutionName: String,
    val projectName: String,
    val targetFrameworkMoniker: String,
    val outputRoot: Path,
    val models: Map<String, DotnetModel>,
    val rest: ResolvedDotnetAspRest,
)
