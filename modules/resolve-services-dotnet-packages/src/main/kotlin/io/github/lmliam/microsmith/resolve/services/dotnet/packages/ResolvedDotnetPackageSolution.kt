package io.github.lmliam.microsmith.resolve.services.dotnet.packages

data class ResolvedDotnetPackageSolution(
    val name: String,
    val packages: List<ResolvedDotnetPackageVersion>,
)
