package io.github.lmliam.microsmith.artifact.services.dotnet.asp.service

import io.github.lmliam.microsmith.artifact.ArtifactId
import kotlin.reflect.KClass

public data class DotnetAspServiceArtifactId(public val solutionName: String, public val projectName: String) :
    ArtifactId<DotnetAspServiceArtifact> {
    override val artifactType: KClass<DotnetAspServiceArtifact> = DotnetAspServiceArtifact::class
}
