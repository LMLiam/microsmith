package io.github.lmliam.microsmith.resolve.services.dotnet.asp

data class ResolvedDotnetAspRest(
    val endpoints: List<ResolvedDotnetAspEndpoint>,
) {
    companion object {
        fun empty() = ResolvedDotnetAspRest(emptyList())
    }
}
