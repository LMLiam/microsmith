package io.github.lmliam.microsmith.artifact.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRest
import java.nio.file.Path

data class DotnetAspServiceContribution(
    override val artifactId: DotnetAspServiceArtifactId,
    val serviceName: String,
    val targetFrameworkMoniker: String,
    val outputRoot: Path,
    val httpPort: Int,
    val httpsPort: Int,
    val models: Map<String, DotnetModel>,
    val rest: ResolvedDotnetAspRest,
) : ArtifactContribution<DotnetAspServiceArtifact>
