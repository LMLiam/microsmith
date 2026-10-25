package io.github.lmliam.microsmith.artifact.services.dotnet.packages.versions

import io.github.lmliam.microsmith.artifact.ArtifactId

data class DotnetPackageVersionsArtifactId(val solutionName: String) : ArtifactId<DotnetPackageVersionsArtifact> {
    override val artifactType = DotnetPackageVersionsArtifact::class
}
