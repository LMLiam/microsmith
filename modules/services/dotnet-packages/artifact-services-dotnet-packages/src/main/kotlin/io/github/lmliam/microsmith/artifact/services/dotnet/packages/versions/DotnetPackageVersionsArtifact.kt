package io.github.lmliam.microsmith.artifact.services.dotnet.packages.versions

import io.github.lmliam.microsmith.artifact.services.dotnet.packages.DotnetPackagesArtifact

public data class DotnetPackageVersionsArtifact(
    override val id: DotnetPackageVersionsArtifactId,
    public val packages: List<DotnetPackageVersion>,
) : DotnetPackagesArtifact
