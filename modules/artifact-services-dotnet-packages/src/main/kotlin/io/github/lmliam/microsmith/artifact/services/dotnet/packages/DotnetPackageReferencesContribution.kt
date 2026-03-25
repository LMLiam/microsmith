package io.github.lmliam.microsmith.artifact.services.dotnet.packages

import io.github.lmliam.microsmith.artifact.core.ArtifactContribution

data class DotnetPackageReferencesContribution(
    override val artifactId: DotnetPackageReferencesArtifactId,
    val solutionName: String,
    val projectName: String,
    val packages: List<String>,
) : ArtifactContribution<DotnetPackageReferencesArtifact>
