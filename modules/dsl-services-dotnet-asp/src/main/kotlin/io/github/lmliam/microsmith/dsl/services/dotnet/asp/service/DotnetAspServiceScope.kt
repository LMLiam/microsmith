package io.github.lmliam.microsmith.dsl.services.dotnet.asp.service

import io.github.lmliam.microsmith.dsl.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.service.DotnetAspRestScope

/** Marker scope for opting a .NET service into ASP.NET scaffolding. */
@MicrosmithDsl
interface DotnetAspServiceScope {
    fun ports(block: DotnetAspPortsScope.() -> Unit)

    fun rest(block: DotnetAspRestScope.() -> Unit)
}
