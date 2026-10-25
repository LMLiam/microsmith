package io.github.lmliam.microsmith.dsl.services.dotnet.asp.service

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.service.DotnetAspServiceBuilder
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.service.DotnetAspServiceExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.service.DotnetAspServiceScope
import io.github.lmliam.microsmith.dsl.services.dotnet.service.DotnetServiceContext
import io.github.lmliam.microsmith.dsl.services.dotnet.service.DotnetServiceScope

/** Start an ASP.NET scaffold block inside a named .NET service. */
fun DotnetServiceScope.asp(block: DotnetAspServiceScope.() -> Unit = {}) {
    val builder = this as? DotnetServiceContext ?: error("asp { ... } can only be invoked within a .NET service block.")

    builder.put(DotnetAspServiceExtension::class, DotnetAspServiceBuilder().apply(block).build())
}

/** Alias for `asp { ... }` when the longer ASP.NET spelling is preferred. */
fun DotnetServiceScope.aspNet(block: DotnetAspServiceScope.() -> Unit = {}) {
    asp(block)
}
