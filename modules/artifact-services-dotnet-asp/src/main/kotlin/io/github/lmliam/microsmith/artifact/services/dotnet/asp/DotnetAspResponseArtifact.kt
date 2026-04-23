package io.github.lmliam.microsmith.artifact.services.dotnet.asp

data class DotnetAspResponseArtifact(
    val statusCode: Int,
    val model: DotnetAspModelArtifact,
    val headers: List<DotnetAspResponseHeaderArtifact>,
    val origins: Set<String>,
)
