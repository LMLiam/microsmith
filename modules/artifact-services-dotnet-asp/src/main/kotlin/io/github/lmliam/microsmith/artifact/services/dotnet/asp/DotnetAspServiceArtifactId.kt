package io.github.lmliam.microsmith.artifact.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.core.ArtifactId

data class DotnetAspServiceArtifactId(val solutionName: String, val projectName: String) :
    ArtifactId<DotnetAspServiceArtifact> {
    override val artifactType = DotnetAspServiceArtifact::class
}
