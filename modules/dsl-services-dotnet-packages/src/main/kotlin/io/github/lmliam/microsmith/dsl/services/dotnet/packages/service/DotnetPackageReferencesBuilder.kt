package io.github.lmliam.microsmith.dsl.services.dotnet.packages.service

import io.github.lmliam.microsmith.dsl.services.dotnet.packages.support.DotnetPackageDeclarationNode
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.support.flattenReferencedPackages
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.support.normalizeDotnetPackagePath
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.support.validateDotnetPackageVersion

internal class DotnetPackageReferencesBuilder(
    private val pathSegments: List<String> = emptyList(),
) : DotnetPackageReferencesScope {
    private var version: String? = null
    private val children = linkedMapOf<String, DotnetPackageReferencesBuilder>()

    override fun version(version: String) {
        this.version = validateDotnetPackageVersion(version, "Package version")
    }

    override fun String.invoke(block: DotnetPackageReferencesScope.() -> Unit) {
        val normalizedPathSegments = normalizeDotnetPackagePath(this, "Package name")
        val childKey = normalizedPathSegments.joinToString(".")
        require(childKey !in children) {
            "Duplicate .NET package declaration for '$childKey'."
        }

        val child = DotnetPackageReferencesBuilder(pathSegments + normalizedPathSegments)
        child.block()
        children[childKey] = child
    }

    override fun String.unaryPlus() {
        this.invoke {}
    }

    fun build(): DotnetPackageReferencesExtension {
        return DotnetPackageReferencesExtension(
            packages = flattenReferencedPackages(buildNode()),
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
