package io.github.lmliam.microsmith.artifact.services.dotnet.asp.request

data class DotnetAspRequestBindingArtifact(
    val typeName: String,
    val name: String,
    val fields: List<DotnetAspRequestFieldArtifact>,
    val origins: Set<String>,
)
