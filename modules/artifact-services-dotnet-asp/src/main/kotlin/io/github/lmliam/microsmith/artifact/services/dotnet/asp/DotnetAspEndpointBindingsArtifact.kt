package io.github.lmliam.microsmith.artifact.services.dotnet.asp

data class DotnetAspEndpointBindingsArtifact(
    val path: DotnetAspRequestBindingArtifact? = null,
    val query: DotnetAspRequestBindingArtifact? = null,
    val headers: DotnetAspHeadersBindingArtifact? = null,
    val body: DotnetAspModelArtifact? = null,
)
