package io.github.lmliam.microsmith.artifact.services.dotnet.asp

data class DotnetAspHeadersBindingArtifact(
    val typeName: String,
    val name: String,
    val headers: List<DotnetAspHeaderFieldArtifact>,
    val origins: Set<String>,
)
