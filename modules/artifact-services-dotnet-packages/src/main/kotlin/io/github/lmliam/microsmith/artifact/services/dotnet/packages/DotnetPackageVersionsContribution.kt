package io.github.lmliam.microsmith.artifact.services.dotnet.packages

import io.github.lmliam.microsmith.artifact.core.ArtifactContribution

data class DotnetPackageVersionsContribution(
    override val artifactId: DotnetPackageVersionsArtifactId,
    val packages: Map<String, String>,
) : ArtifactContribution<DotnetPackageVersionsArtifact>
