package io.github.lmliam.microsmith.dsl.services.dotnet.packages.support

internal data class DotnetPackageNode(
    val pathSegments: List<String>,
    val version: String?,
    val children: List<DotnetPackageNode>,
)
