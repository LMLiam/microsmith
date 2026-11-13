package io.github.lmliam.microsmith.resolve.services.dotnet.packages.resolution

import io.github.lmliam.microsmith.dsl.services.dotnet.packages.solution.DotnetPackageVersionDeclaration
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.ResolvedDotnetPackageVersion
import tech.mappie.api.ObjectMappie

internal object DotnetPackageVersionMapper :
    ObjectMappie<DotnetPackageVersionDeclaration, ResolvedDotnetPackageVersion>()
