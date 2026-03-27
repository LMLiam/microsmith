package io.github.lmliam.microsmith.artifact.services.dotnet.packages

import io.github.lmliam.microsmith.artifact.services.dotnet.packages.core.DotnetPackagesArtifact

data class DotnetPackageVersionsArtifact(
    override val id: DotnetPackageVersionsArtifactId,
    val packages: List<DotnetPackageVersion>,
) : DotnetPackagesArtifact
