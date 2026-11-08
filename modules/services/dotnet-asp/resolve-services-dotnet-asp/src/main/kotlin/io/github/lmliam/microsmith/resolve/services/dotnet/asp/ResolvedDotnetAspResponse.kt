package io.github.lmliam.microsmith.resolve.services.dotnet.asp

data class ResolvedDotnetAspResponse(
    val statusCode: Int,
    val model: ResolvedDotnetAspModel,
    val headers: List<ResolvedDotnetAspResponseHeader>,
)
