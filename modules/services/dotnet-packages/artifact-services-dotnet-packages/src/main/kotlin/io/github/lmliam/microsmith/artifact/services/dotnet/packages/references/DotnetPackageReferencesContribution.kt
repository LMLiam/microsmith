package io.github.lmliam.microsmith.artifact.services.dotnet.packages.references

import io.github.lmliam.microsmith.artifact.ArtifactContribution

data class DotnetPackageReferencesContribution(
    override val artifactId: DotnetPackageReferencesArtifactId,
    val solutionName: String,
    val projectName: String,
    val packages: List<DotnetPackageReference>,
) : ArtifactContribution<DotnetPackageReferencesArtifact>
