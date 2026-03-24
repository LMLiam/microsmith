package io.github.lmliam.microsmith.dsl.services.dotnet.core

import io.github.lmliam.microsmith.dsl.services.core.ServiceBuilder
import io.github.lmliam.microsmith.dsl.services.core.ServiceScope
import io.github.lmliam.microsmith.dsl.services.core.ServicesBuilder
import io.github.lmliam.microsmith.dsl.services.core.ServicesScope
import io.github.lmliam.microsmith.dsl.services.dotnet.core.defaults.DotnetDefaultsBuilder
import io.github.lmliam.microsmith.dsl.services.dotnet.core.defaults.DotnetDefaultsExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.defaults.DotnetDefaultsScope
import io.github.lmliam.microsmith.dsl.services.dotnet.core.service.DotnetServiceBuilder
import io.github.lmliam.microsmith.dsl.services.dotnet.core.service.DotnetServiceExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.service.DotnetServiceScope

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
