package io.github.lmliam.microsmith.artifact.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.core.ServicesArtifact
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRest
import java.nio.file.Path

data class DotnetAspServiceArtifact(
    override val id: DotnetAspServiceArtifactId,
    val serviceName: String,
    val targetFrameworkMoniker: String,
    val outputRoot: Path,
    val httpPort: Int,
    val httpsPort: Int,
    val models: Map<String, DotnetModel>,
    val rest: ResolvedDotnetAspRest,
) : ServicesArtifact
