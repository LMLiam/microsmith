package io.github.lmliam.microsmith.resolve.services.dotnet.asp.routing

internal sealed interface DotnetAspRouteSegment {
    val text: String

    data class Literal(override val text: String) : DotnetAspRouteSegment

    data class Placeholder(val name: String) : DotnetAspRouteSegment {
        override val text = "{$name}"
    }
}
