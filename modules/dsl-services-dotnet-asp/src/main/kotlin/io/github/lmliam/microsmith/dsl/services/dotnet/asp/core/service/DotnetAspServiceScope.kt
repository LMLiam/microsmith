package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.service

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.service.DotnetAspRestScope

/**
 * Marker scope for opting a .NET service into ASP.NET scaffolding.
 */
@MicrosmithDsl
interface DotnetAspServiceScope {
    fun rest(block: DotnetAspRestScope.() -> Unit)
}
