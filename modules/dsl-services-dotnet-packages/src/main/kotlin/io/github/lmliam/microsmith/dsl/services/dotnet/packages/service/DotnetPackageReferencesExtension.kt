package io.github.lmliam.microsmith.dsl.services.dotnet.packages.service

import io.github.lmliam.microsmith.dsl.core.MergeableExtension
import io.github.lmliam.microsmith.dsl.services.core.ServiceExtension

/**
 * Per-project package references declared under `packages { ... }` inside a .NET service block.
 */
data class DotnetPackageReferencesExtension(
    val packages: Set<String> = emptySet(),
) : ServiceExtension, MergeableExtension<DotnetPackageReferencesExtension> {
    fun findPackage(name: String) = packages.find { it == name }

    fun requirePackage(name: String): String {
        require(name.isNotBlank()) { "Package name cannot be blank." }
        return findPackage(name) ?: error("Dotnet package not found: $name")
    }

    override fun merge(other: DotnetPackageReferencesExtension): DotnetPackageReferencesExtension {
        val collisions = other.packages.filter { it in packages }.sorted()

        require(collisions.isEmpty()) {
            "Duplicate .NET package references while merging service configuration: ${collisions.joinToString(", ")}"
        }

        return copy(packages = packages + other.packages)
    }
}
