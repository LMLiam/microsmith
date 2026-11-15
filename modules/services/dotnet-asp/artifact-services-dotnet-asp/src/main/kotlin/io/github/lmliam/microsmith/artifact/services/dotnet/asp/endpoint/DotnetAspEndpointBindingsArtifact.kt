package io.github.lmliam.microsmith.artifact.services.dotnet.asp.endpoint

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.model.DotnetAspModelArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.request.DotnetAspHeadersBindingArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.request.DotnetAspRequestBindingArtifact

public data class DotnetAspEndpointBindingsArtifact(
    public val path: DotnetAspRequestBindingArtifact? = null,
    public val query: DotnetAspRequestBindingArtifact? = null,
    public val headers: DotnetAspHeadersBindingArtifact? = null,
    public val body: DotnetAspModelArtifact? = null,
)
