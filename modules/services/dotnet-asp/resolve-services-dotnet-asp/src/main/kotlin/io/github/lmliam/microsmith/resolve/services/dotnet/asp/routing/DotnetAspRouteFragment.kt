package io.github.lmliam.microsmith.resolve.services.dotnet.asp.routing

internal data class DotnetAspRouteFragment(val segments: List<DotnetAspRouteSegment>) {
    operator fun plus(other: DotnetAspRouteFragment): DotnetAspRouteFragment =
        DotnetAspRouteFragment(segments + other.segments)

    companion object {
        val Empty = DotnetAspRouteFragment(emptyList())
    }
}
