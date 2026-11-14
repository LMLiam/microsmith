package io.github.lmliam.microsmith.artifact.services.dotnet.packages

import io.github.lmliam.microsmith.artifact.services.dotnet.packages.versions.DotnetPackageVersion
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.ResolvedDotnetPackageVersion
import tech.mappie.api.ObjectMappie

internal object DotnetPackageVersionMapper : ObjectMappie<ResolvedDotnetPackageVersion, DotnetPackageVersion>()
