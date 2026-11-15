package io.github.lmliam.microsmith.artifact.services.dotnet.asp.request

public data class DotnetAspHeadersBindingArtifact(
    public val typeName: String,
    public val name: String,
    public val headers: List<DotnetAspHeaderFieldArtifact>,
)
