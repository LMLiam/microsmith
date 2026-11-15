package io.github.lmliam.microsmith.artifact.services.dotnet.asp.service

import io.github.lmliam.microsmith.artifact.files.RelativePath
import io.github.lmliam.microsmith.artifact.services.ServicesArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.endpoint.DotnetAspEndpointArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.model.DotnetAspModelArtifact

public data class DotnetAspServiceArtifact(
    override val id: DotnetAspServiceArtifactId,
    public val serviceName: String,
    public val targetFrameworkMoniker: String,
    public val outputRoot: RelativePath,
    public val httpPort: Int,
    public val httpsPort: Int,
    public val contractModels: List<DotnetAspModelArtifact>,
    public val endpoints: List<DotnetAspEndpointArtifact>,
) : ServicesArtifact
