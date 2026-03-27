package io.github.lmliam.microsmith.dsl.services.dotnet.packages.support

internal data class DotnetPackageDeclarationNode(
    val pathSegments: List<String>,
    val version: String?,
    val childPackages: List<DotnetPackageDeclarationNode>,
)
