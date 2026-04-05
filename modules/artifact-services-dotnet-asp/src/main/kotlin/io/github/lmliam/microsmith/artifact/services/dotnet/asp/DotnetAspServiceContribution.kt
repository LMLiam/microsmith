package io.github.lmliam.microsmith.artifact.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import java.nio.file.Path

data class DotnetAspServiceContribution(
    override val artifactId: DotnetAspServiceArtifactId,
    val serviceName: String,
    val targetFrameworkMoniker: String,
    val outputRoot: Path,
    val httpPort: Int,
    val httpsPort: Int,
) : ArtifactContribution<DotnetAspServiceArtifact>
