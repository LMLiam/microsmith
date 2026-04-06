package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.service

import io.github.lmliam.microsmith.dsl.core.MergeableExtension
import io.github.lmliam.microsmith.dsl.services.core.ServiceExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.service.DotnetAspRest
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.service.mergeDotnetAspRest

/**
 * Per-service ASP.NET scaffold opt-in declared under `asp { ... }`.
 */
data class DotnetAspServiceExtension(
    val ports: DotnetAspPorts? = null,
    val rest: DotnetAspRest? = null,
) :
    ServiceExtension,
    MergeableExtension<DotnetAspServiceExtension> {
    override fun merge(other: DotnetAspServiceExtension) = DotnetAspServiceExtension(
        ports =
        when {
            ports == null -> other.ports
            other.ports == null -> ports
            else -> mergeDotnetAspPorts(ports, other.ports)
        },
        rest =
        when {
            rest == null -> other.rest
            other.rest == null -> rest
            else -> mergeDotnetAspRest(rest, other.rest)
        },
    )
}
