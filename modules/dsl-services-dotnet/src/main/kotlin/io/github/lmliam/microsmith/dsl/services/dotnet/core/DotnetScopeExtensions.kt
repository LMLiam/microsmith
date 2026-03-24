package io.github.lmliam.microsmith.dsl.services.dotnet.core

import io.github.lmliam.microsmith.dsl.services.core.ServiceBuilder
import io.github.lmliam.microsmith.dsl.services.core.ServiceScope
import io.github.lmliam.microsmith.dsl.services.core.ServicesBuilder
import io.github.lmliam.microsmith.dsl.services.core.ServicesScope
import io.github.lmliam.microsmith.dsl.services.dotnet.core.service.DotnetServiceBuilder
import io.github.lmliam.microsmith.dsl.services.dotnet.core.service.DotnetServiceExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.service.DotnetServiceScope
import io.github.lmliam.microsmith.dsl.services.dotnet.core.shared.DotnetSharedBuilder
import io.github.lmliam.microsmith.dsl.services.dotnet.core.shared.DotnetSharedExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.shared.DotnetSharedScope

/**
 * Start a shared .NET defaults block inside `services { ... }`.
 */
fun ServicesScope.dotnet(block: DotnetSharedScope.() -> Unit) {
    val builder =
        this as? ServicesBuilder
            ?: error("dotnet { ... } can only be invoked within a services { ... } block.")

    builder.put(DotnetSharedExtension::class, DotnetSharedBuilder().apply(block).build())
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
