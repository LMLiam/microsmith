package io.github.lmliam.microsmith.dsl.services.dotnet.packages.solution

import io.github.lmliam.microsmith.dsl.MergeableExtension
import io.github.lmliam.microsmith.dsl.MicrosmithExtension

/** Central package versions declared under `solutions { "Name" { packages { ... } } }`. */
data class DotnetPackageVersionsExtension(val packages: List<DotnetPackageVersionDeclaration> = emptyList()) :
    MicrosmithExtension, MergeableExtension<DotnetPackageVersionsExtension> {
    override fun merge(other: DotnetPackageVersionsExtension): DotnetPackageVersionsExtension {
        val existingNames = packages.map(DotnetPackageVersionDeclaration::name).toSet()
        val collisions =
            other.packages.map(DotnetPackageVersionDeclaration::name).filter(existingNames::contains).sorted()

        require(collisions.isEmpty()) {
            "Duplicate .NET package ownership while merging solution configuration: ${collisions.joinToString(", ")}"
        }

        return copy(packages = packages + other.packages)
    }
}
