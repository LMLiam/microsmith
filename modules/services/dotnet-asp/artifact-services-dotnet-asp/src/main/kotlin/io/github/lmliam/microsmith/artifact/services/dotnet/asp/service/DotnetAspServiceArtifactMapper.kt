package io.github.lmliam.microsmith.artifact.services.dotnet.asp.service

import tech.mappie.api.ObjectMappie

internal object DotnetAspServiceArtifactMapper :
    ObjectMappie<DotnetAspServiceContribution, DotnetAspServiceArtifact>() {
    override fun map(from: DotnetAspServiceContribution): DotnetAspServiceArtifact = mapping {
        to::id fromProperty from::artifactId
    }
}
