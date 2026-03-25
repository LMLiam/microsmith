package io.github.lmliam.microsmith.gen.services.dotnet.packages

data class ResolvedDotnetPackageSolution(
    val name: String,
    val packages: Map<String, String>,
)
