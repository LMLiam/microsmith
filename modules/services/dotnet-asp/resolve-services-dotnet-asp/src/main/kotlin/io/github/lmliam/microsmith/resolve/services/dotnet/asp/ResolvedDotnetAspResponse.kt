package io.github.lmliam.microsmith.resolve.services.dotnet.asp

public data class ResolvedDotnetAspResponse(
    public val statusCode: Int,
    public val model: ResolvedDotnetAspModel,
    public val headers: List<ResolvedDotnetAspResponseHeader>,
)
