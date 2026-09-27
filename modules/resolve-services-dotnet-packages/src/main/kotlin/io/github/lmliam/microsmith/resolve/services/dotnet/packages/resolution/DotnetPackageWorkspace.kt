package io.github.lmliam.microsmith.resolve.services.dotnet.packages.resolution

import io.github.lmliam.microsmith.resolve.ResolvedModel
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.ResolvedDotnetPackageService
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.ResolvedDotnetPackageSolution

data class DotnetPackageWorkspace(
    val solutionsByName: Map<String, ResolvedDotnetPackageSolution>,
    val servicesByName: Map<String, ResolvedDotnetPackageService>,
) : ResolvedModel {
    fun findSolution(name: String): ResolvedDotnetPackageSolution? = solutionsByName[name]

    fun requireSolution(name: String): ResolvedDotnetPackageSolution =
        requireNotNull(findSolution(name)) { "Resolved .NET package solution not found: $name" }

    fun findService(name: String): ResolvedDotnetPackageService? = servicesByName[name]

    fun requireService(name: String): ResolvedDotnetPackageService =
        requireNotNull(findService(name)) { "Resolved .NET package service not found: $name" }
}
