package io.github.lmliam.microsmith.artifact.services.dotnet.asp.service

import io.github.lmliam.microsmith.artifact.ArtifactId

data class DotnetAspServiceArtifactId(val solutionName: String, val projectName: String) :
    ArtifactId<DotnetAspServiceArtifact> {
    override val artifactType = DotnetAspServiceArtifact::class
}
