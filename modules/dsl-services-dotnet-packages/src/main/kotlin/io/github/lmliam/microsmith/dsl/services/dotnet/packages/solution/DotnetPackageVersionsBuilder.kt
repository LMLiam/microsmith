package io.github.lmliam.microsmith.dsl.services.dotnet.packages.solution

import io.github.lmliam.microsmith.dsl.services.dotnet.packages.support.DotnetPackageDeclarationNode
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.support.flattenOwnedPackages
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.support.normalizeDotnetPackagePath
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.support.validateDotnetPackageVersion

internal class DotnetPackageVersionsBuilder(
    private val pathSegments: List<String> = emptyList(),
) : DotnetPackageVersionScope {
    private var version: String? = null
    private val children = linkedMapOf<String, DotnetPackageVersionsBuilder>()

    override fun version(version: String) {
        this.version = validateDotnetPackageVersion(version, "Package version")
    }

    override fun String.invoke(block: DotnetPackageVersionScope.() -> Unit) {
        val normalizedPathSegments = normalizeDotnetPackagePath(this, "Package name")
        val childKey = normalizedPathSegments.joinToString(".")
        require(childKey !in children) {
            "Duplicate .NET package declaration for '$childKey'."
        }

        val child = DotnetPackageVersionsBuilder(pathSegments + normalizedPathSegments)
        child.block()
        children[childKey] = child
    }

    override fun String.unaryPlus() {
        this.invoke {}
    }

    fun build(): DotnetPackageVersionsExtension {
        return DotnetPackageVersionsExtension(
            packages = flattenOwnedPackages(buildNode()),
        )
    }

    private fun buildNode(): DotnetPackageDeclarationNode {
        return DotnetPackageDeclarationNode(
            pathSegments = pathSegments,
            version = version,
            childPackages = children.values.map { it.buildNode() },
        )
    }
}
