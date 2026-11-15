package io.github.lmliam.microsmith.artifact.services.dotnet.asp.request

public data class DotnetAspRequestBindingArtifact(
    public val typeName: String,
    public val name: String,
    public val fields: List<DotnetAspRequestFieldArtifact>,
)
