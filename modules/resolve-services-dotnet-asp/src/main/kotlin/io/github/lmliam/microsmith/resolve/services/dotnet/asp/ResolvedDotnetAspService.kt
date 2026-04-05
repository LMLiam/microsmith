package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import java.nio.file.Path

data class ResolvedDotnetAspService(
    val name: String,
    val solutionName: String,
    val projectName: String,
    val targetFrameworkMoniker: String,
    val outputRoot: Path,
)
