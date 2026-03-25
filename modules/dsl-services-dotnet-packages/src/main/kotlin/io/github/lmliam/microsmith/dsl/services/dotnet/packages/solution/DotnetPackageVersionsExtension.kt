package io.github.lmliam.microsmith.dsl.services.dotnet.packages.solution

import io.github.lmliam.microsmith.dsl.core.MergeableExtension
import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension

/**
 * Central package versions declared under `solutions { "Name" { packages { ... } } }`.
 */
data class DotnetPackageVersionsExtension(
    val packages: Map<String, String> = emptyMap(),
) : MicrosmithExtension, MergeableExtension<DotnetPackageVersionsExtension> {
    fun findVersion(name: String) = packages[name]

    fun requireVersion(name: String): String {
        require(name.isNotBlank()) { "Package name cannot be blank." }
        return findVersion(name) ?: error("Dotnet package version not found: $name")
    }

    override fun merge(other: DotnetPackageVersionsExtension): DotnetPackageVersionsExtension {
        val collisions = other.packages.keys.filter { it in packages }.sorted()

        require(collisions.isEmpty()) {
            "Duplicate .NET package ownership while merging solution configuration: ${collisions.joinToString(", ")}"
        }

        return copy(packages = packages + other.packages)
    }
}
