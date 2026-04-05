package io.github.lmliam.microsmith.dsl.services.dotnet.core.service

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.service.DotnetAspServiceBuilder
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.service.DotnetAspServiceExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.service.DotnetAspServiceScope

/**
 * Start an ASP.NET scaffold block inside a named .NET service.
 */
fun DotnetServiceScope.asp(block: DotnetAspServiceScope.() -> Unit = {}) {
    val builder =
        this as? DotnetServiceContext
            ?: error("asp { ... } can only be invoked within a .NET service block.")

    builder.put(DotnetAspServiceExtension::class, DotnetAspServiceBuilder().apply(block).build())
}
