package io.github.lmliam.microsmith.artifact.services.dotnet.packages.versions

import io.github.lmliam.microsmith.artifact.ArtifactContribution

data class DotnetPackageVersionsContribution(
    override val artifactId: DotnetPackageVersionsArtifactId,
    val packages: List<DotnetPackageVersion>,
) : ArtifactContribution<DotnetPackageVersionsArtifact>
