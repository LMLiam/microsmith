package io.github.lmliam.microsmith.resolve.services.dotnet.asp

public data class ResolvedDotnetAspHeadersBinding(
    public val name: String,
    public val headers: List<ResolvedDotnetAspHeaderField>,
)
