package io.github.lmliam.microsmith.artifact.services.dotnet.asp.service

import io.github.lmliam.microsmith.artifact.services.ServicesArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.endpoint.DotnetAspEndpointArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.model.DotnetAspModelArtifact
import java.nio.file.Path

data class DotnetAspServiceArtifact(
    override val id: DotnetAspServiceArtifactId,
    val serviceName: String,
    val targetFrameworkMoniker: String,
    val outputRoot: Path,
    val httpPort: Int,
    val httpsPort: Int,
    val contractModels: List<DotnetAspModelArtifact>,
    val endpoints: List<DotnetAspEndpointArtifact>,
) : ServicesArtifact
