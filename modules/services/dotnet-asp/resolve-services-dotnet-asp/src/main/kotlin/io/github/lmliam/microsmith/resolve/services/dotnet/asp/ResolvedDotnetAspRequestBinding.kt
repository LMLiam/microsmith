package io.github.lmliam.microsmith.resolve.services.dotnet.asp

public data class ResolvedDotnetAspRequestBinding(
    public val name: String,
    public val fields: List<ResolvedDotnetAspRequestField>,
)
