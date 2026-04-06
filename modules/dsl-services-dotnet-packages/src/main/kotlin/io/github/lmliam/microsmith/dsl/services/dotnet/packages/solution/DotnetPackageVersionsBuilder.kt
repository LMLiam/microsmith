package io.github.lmliam.microsmith.dsl.services.dotnet.packages.solution

import io.github.lmliam.microsmith.dsl.services.dotnet.packages.support.normalizeDotnetPackagePath
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.support.validateDotnetPackageVersion

internal class DotnetPackageVersionsBuilder(private val pathSegments: List<String> = emptyList()) :
    DotnetPackageVersionScope {
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
        val packages = linkedMapOf<String, DotnetPackageVersionDeclaration>()
        collectPackages(inheritedVersion = null, packages = packages)
        return DotnetPackageVersionsExtension(packages = packages.values.toList())
    }

    private fun collectPackages(
        inheritedVersion: String?,
        packages: MutableMap<String, DotnetPackageVersionDeclaration>,
    ) {
        val currentVersion = version ?: inheritedVersion

        if (pathSegments.isNotEmpty() && children.isEmpty()) {
            val packageName = pathSegments.joinToString(".")
            val resolvedVersion =
                currentVersion
                    ?: error("Dotnet package '$packageName' must declare a version.")

            require(packageName !in packages) {
                "Duplicate .NET package ownership declaration for '$packageName'."
            }

            packages[packageName] = DotnetPackageVersionDeclaration(name = packageName, version = resolvedVersion)
            return
        }

        children.values.forEach { child ->
            child.collectPackages(inheritedVersion = currentVersion, packages = packages)
        }
    }
}
