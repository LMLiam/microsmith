package io.github.lmliam.microsmith.dsl.services.dotnet.packages.service

import io.github.lmliam.microsmith.dsl.core.MergeableExtension
import io.github.lmliam.microsmith.dsl.services.core.ServiceExtension

/**
 * Per-project package references declared under `packages { ... }` inside a .NET service block.
 * A null value means the package reference is versionless at the service level and must be
 * satisfied by central package ownership during resolution.
 */
data class DotnetPackageReferencesExtension(val packages: List<DotnetPackageReferenceDeclaration> = emptyList()) :
    ServiceExtension,
    MergeableExtension<DotnetPackageReferencesExtension> {
    fun findPackage(name: String): String? = packages.find { it.name == name }?.version

    fun requirePackage(name: String): String {
        require(name.isNotBlank()) { "Package name cannot be blank." }
        return findPackage(name) ?: error("Dotnet package not found: $name")
    }

    override fun merge(other: DotnetPackageReferencesExtension): DotnetPackageReferencesExtension {
        val existingNames = packages.map(DotnetPackageReferenceDeclaration::name).toSet()
        val collisions =
            other.packages
                .map(DotnetPackageReferenceDeclaration::name)
                .filter(existingNames::contains)
                .sorted()

        require(collisions.isEmpty()) {
            "Duplicate .NET package references while merging service configuration: ${collisions.joinToString(", ")}"
        }

        return copy(packages = packages + other.packages)
    }
}
