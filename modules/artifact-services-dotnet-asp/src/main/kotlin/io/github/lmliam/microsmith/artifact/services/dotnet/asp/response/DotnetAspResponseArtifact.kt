package io.github.lmliam.microsmith.artifact.services.dotnet.asp.response
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.model.DotnetAspModelArtifact
data class DotnetAspResponseArtifact(
    val statusCode: Int,
    val model: DotnetAspModelArtifact,
    val headers: List<DotnetAspResponseHeaderArtifact>,
    val origins: Set<String>,
)
