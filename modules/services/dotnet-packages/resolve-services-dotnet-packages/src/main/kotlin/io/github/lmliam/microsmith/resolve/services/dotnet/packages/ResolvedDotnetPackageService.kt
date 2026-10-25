package io.github.lmliam.microsmith.resolve.services.dotnet.packages

data class ResolvedDotnetPackageService(
    val name: String,
    val solution: String,
    val project: String,
    val packages: List<ResolvedDotnetPackageReference>,
)
