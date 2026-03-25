package io.github.lmliam.microsmith.gen.services.dotnet.packages

data class ResolvedDotnetPackageService(
    val name: String,
    val solution: String,
    val project: String,
    val packages: Map<String, String>,
)
