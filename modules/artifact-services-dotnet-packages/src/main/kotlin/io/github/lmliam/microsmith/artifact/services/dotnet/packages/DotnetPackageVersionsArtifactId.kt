package io.github.lmliam.microsmith.artifact.services.dotnet.packages

import io.github.lmliam.microsmith.artifact.core.ArtifactId

data class DotnetPackageVersionsArtifactId(val solutionName: String) : ArtifactId<DotnetPackageVersionsArtifact> {
    override val artifactType = DotnetPackageVersionsArtifact::class
}
