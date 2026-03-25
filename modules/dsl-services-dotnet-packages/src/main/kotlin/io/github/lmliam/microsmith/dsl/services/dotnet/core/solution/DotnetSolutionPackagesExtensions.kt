package io.github.lmliam.microsmith.dsl.services.dotnet.core.solution

import io.github.lmliam.microsmith.dsl.services.dotnet.packages.solution.DotnetPackageVersionsBuilder
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.solution.DotnetPackageVersionsExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.solution.DotnetPackageVersionsScope

/**
 * Start a central package ownership block inside a named .NET solution.
 */
fun DotnetSolutionScope.packages(block: DotnetPackageVersionsScope.() -> Unit) {
    val builder =
        this as? DotnetSolutionContext
            ?: error("packages { ... } can only be invoked within a .NET solution block.")

    builder.put(DotnetPackageVersionsExtension::class, DotnetPackageVersionsBuilder().apply(block).build())
}
