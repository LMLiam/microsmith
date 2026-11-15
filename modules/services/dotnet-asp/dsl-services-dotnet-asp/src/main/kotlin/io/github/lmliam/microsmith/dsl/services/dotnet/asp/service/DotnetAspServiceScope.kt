package io.github.lmliam.microsmith.dsl.services.dotnet.asp.service

import io.github.lmliam.microsmith.dsl.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.service.DotnetAspRestScope

/** Marker scope for opting a .NET service into ASP.NET scaffolding. */
@MicrosmithDsl
public interface DotnetAspServiceScope {
    public fun ports(block: DotnetAspPortsScope.() -> Unit)

    public fun rest(block: DotnetAspRestScope.() -> Unit)
}
