package io.github.lmliam.microsmith.artifact.services.dotnet.asp.endpoint
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.model.DotnetAspModelArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.request.DotnetAspHeadersBindingArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.request.DotnetAspRequestBindingArtifact
data class DotnetAspEndpointBindingsArtifact(
    val path: DotnetAspRequestBindingArtifact? = null,
    val query: DotnetAspRequestBindingArtifact? = null,
    val headers: DotnetAspHeadersBindingArtifact? = null,
    val body: DotnetAspModelArtifact? = null,
)
