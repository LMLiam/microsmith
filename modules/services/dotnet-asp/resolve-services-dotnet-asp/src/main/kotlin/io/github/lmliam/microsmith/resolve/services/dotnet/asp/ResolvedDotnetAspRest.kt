package io.github.lmliam.microsmith.resolve.services.dotnet.asp

public data class ResolvedDotnetAspRest(public val endpoints: List<ResolvedDotnetAspEndpoint>) {
    public companion object {
        public fun empty(): ResolvedDotnetAspRest = ResolvedDotnetAspRest(emptyList())
    }
}
