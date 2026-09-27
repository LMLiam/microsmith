package io.github.lmliam.microsmith.artifact.services.dotnet.asp.service

import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.endpoint.DotnetAspEndpointArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.model.DotnetAspModelArtifact
import java.nio.file.Path

data class DotnetAspServiceContribution(
    override val artifactId: DotnetAspServiceArtifactId,
    val serviceName: String,
    val targetFrameworkMoniker: String,
    val outputRoot: Path,
    val httpPort: Int,
    val httpsPort: Int,
    val contractModels: List<DotnetAspModelArtifact>,
    val endpoints: List<DotnetAspEndpointArtifact>,
) : ArtifactContribution<DotnetAspServiceArtifact>
