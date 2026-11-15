package io.github.lmliam.microsmith.resolve.services.dotnet.packages.resolution

import io.github.lmliam.microsmith.resolve.services.ResolvedServicesModel
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.ResolvedDotnetPackageService
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.ResolvedDotnetPackageSolution

public data class DotnetPackageWorkspace(
    public val solutionsByName: Map<String, ResolvedDotnetPackageSolution>,
    public val servicesByName: Map<String, ResolvedDotnetPackageService>,
) : ResolvedServicesModel {
    internal fun findSolution(name: String): ResolvedDotnetPackageSolution? = solutionsByName[name]

    internal fun requireSolution(name: String): ResolvedDotnetPackageSolution =
        requireNotNull(findSolution(name)) { "Resolved .NET package solution not found: $name" }

    internal fun findService(name: String): ResolvedDotnetPackageService? = servicesByName[name]

    internal fun requireService(name: String): ResolvedDotnetPackageService =
        requireNotNull(findService(name)) { "Resolved .NET package service not found: $name" }
}
