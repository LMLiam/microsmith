package io.github.lmliam.microsmith.resolve.services.dotnet.packages

import io.github.lmliam.microsmith.resolve.core.ResolvedModel

data class DotnetPackageWorkspace(
    val solutions: Map<String, ResolvedDotnetPackageSolution>,
    val services: Map<String, ResolvedDotnetPackageService>,
) : ResolvedModel
