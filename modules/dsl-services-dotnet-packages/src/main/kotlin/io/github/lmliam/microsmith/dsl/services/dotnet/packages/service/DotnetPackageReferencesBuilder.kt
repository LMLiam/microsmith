package io.github.lmliam.microsmith.dsl.services.dotnet.packages.service

import io.github.lmliam.microsmith.dsl.services.dotnet.packages.support.normalizeDotnetPackagePath
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.support.validateDotnetPackageVersion

internal class DotnetPackageReferencesBuilder(private val pathSegments: List<String> = emptyList()) :
    DotnetPackageReferencesScope {
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
        val packages = linkedMapOf<String, DotnetPackageReferenceDeclaration>()
        collectPackages(inheritedVersion = null, packages = packages)
        return DotnetPackageReferencesExtension(packages = packages.values.toList())
    }

    private fun collectPackages(
        inheritedVersion: String?,
        packages: MutableMap<String, DotnetPackageReferenceDeclaration>,
    ) {
        val currentVersion = version ?: inheritedVersion

        if (pathSegments.isNotEmpty() && children.isEmpty()) {
            val packageName = pathSegments.joinToString(".")

            require(packageName !in packages) {
                "Duplicate .NET package reference declaration for '$packageName'."
            }

            packages[packageName] = DotnetPackageReferenceDeclaration(name = packageName, version = currentVersion)
            return
        }

        children.values.forEach { child ->
            child.collectPackages(inheritedVersion = currentVersion, packages = packages)
        }
    }
}
