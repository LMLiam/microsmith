package io.github.lmliam.microsmith.dsl.services.dotnet.asp.service

import io.github.lmliam.microsmith.dsl.MergeableExtension
import io.github.lmliam.microsmith.dsl.services.ServiceExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.service.DotnetAspRest
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.service.mergeDotnetAspRest

/** Per-service ASP.NET scaffold opt-in declared under `asp { ... }`. */
public data class DotnetAspServiceExtension(
    public val ports: DotnetAspPorts? = null,
    public val rest: DotnetAspRest? = null,
) : ServiceExtension, MergeableExtension<DotnetAspServiceExtension> {
    override fun merge(other: DotnetAspServiceExtension): DotnetAspServiceExtension =
        DotnetAspServiceExtension(
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
