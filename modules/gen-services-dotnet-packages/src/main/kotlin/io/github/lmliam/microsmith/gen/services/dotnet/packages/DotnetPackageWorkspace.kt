package io.github.lmliam.microsmith.gen.services.dotnet.packages

data class DotnetPackageWorkspace(
    val solutions: Map<String, ResolvedDotnetPackageSolution>,
    val services: Map<String, ResolvedDotnetPackageService>,
)
