package io.github.lmliam.microsmith.dsl.services.dotnet
import io.github.lmliam.microsmith.dsl.services.ServiceBuilder
import io.github.lmliam.microsmith.dsl.services.ServiceScope
import io.github.lmliam.microsmith.dsl.services.ServicesBuilder
import io.github.lmliam.microsmith.dsl.services.ServicesScope
import io.github.lmliam.microsmith.dsl.services.dotnet.defaults.DotnetDefaultsBuilder
import io.github.lmliam.microsmith.dsl.services.dotnet.defaults.DotnetDefaultsExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.defaults.DotnetDefaultsScope
import io.github.lmliam.microsmith.dsl.services.dotnet.service.DotnetServiceBuilder
import io.github.lmliam.microsmith.dsl.services.dotnet.service.DotnetServiceExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.service.DotnetServiceScope

/**
 * Start a shared .NET defaults block inside `services { ... }`.
 */
fun ServicesScope.dotnet(block: DotnetDefaultsScope.() -> Unit) {
    val builder =
        this as? ServicesBuilder
            ?: error("dotnet { ... } can only be invoked within a services { ... } block.")

    builder.put(DotnetDefaultsExtension::class, DotnetDefaultsBuilder().apply(block).build())
}

/**
 * Start a per-service .NET configuration block inside a named service.
 */
fun ServiceScope.dotnet(block: DotnetServiceScope.() -> Unit) {
    val builder =
        this as? ServiceBuilder
            ?: error("dotnet { ... } can only be invoked within a service block.")

    builder.put(DotnetServiceExtension::class, DotnetServiceBuilder().apply(block).build())
}
