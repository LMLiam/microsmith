package io.github.lmliam.microsmith.dsl.services.dotnet.core.service

import io.github.lmliam.microsmith.dsl.services.dotnet.packages.service.DotnetPackageReferencesBuilder
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.service.DotnetPackageReferencesExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.service.DotnetPackageReferencesScope

/**
 * Start a per-project package references block inside a named .NET service.
 */
fun DotnetServiceScope.packages(block: DotnetPackageReferencesScope.() -> Unit) {
    val builder =
        this as? DotnetServiceContext
            ?: error("packages { ... } can only be invoked within a .NET service block.")

    builder.put(DotnetPackageReferencesExtension::class, DotnetPackageReferencesBuilder().apply(block).build())
}
