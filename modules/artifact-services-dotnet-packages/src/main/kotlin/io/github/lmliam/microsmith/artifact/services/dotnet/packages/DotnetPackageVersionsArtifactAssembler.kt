package io.github.lmliam.microsmith.artifact.services.dotnet.packages

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.ArtifactAssembler
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution

@ServiceProvider(ArtifactAssembler::class)
class DotnetPackageVersionsArtifactAssembler : ArtifactAssembler<DotnetPackageVersionsArtifact> {
    override val artifactType = DotnetPackageVersionsArtifact::class

    override fun create(first: ArtifactContribution<DotnetPackageVersionsArtifact>): DotnetPackageVersionsArtifact {
        val contribution = requireContribution(first)
        return DotnetPackageVersionsArtifact(
            id = contribution.artifactId,
            packages = mergePackages(emptyList(), contribution.packages, contribution.artifactId.solutionName),
        )
    }

    override fun merge(
        current: DotnetPackageVersionsArtifact,
        contribution: ArtifactContribution<DotnetPackageVersionsArtifact>,
    ): DotnetPackageVersionsArtifact {
        val next = requireContribution(contribution)
        return current.copy(
            packages = mergePackages(current.packages, next.packages, current.id.solutionName),
        )
    }

    private fun requireContribution(
        contribution: ArtifactContribution<DotnetPackageVersionsArtifact>,
    ): DotnetPackageVersionsContribution {
        require(contribution is DotnetPackageVersionsContribution) {
            "Unsupported dotnet package versions contribution type: ${contribution::class}"
        }
        return contribution
    }

    private fun mergePackages(
        current: List<DotnetPackageVersion>,
        next: List<DotnetPackageVersion>,
        solutionName: String,
    ): List<DotnetPackageVersion> {
        val merged = LinkedHashMap(current.associateBy(DotnetPackageVersion::name))
        next.forEach { packageVersion ->
            val existing = merged[packageVersion.name]
            require(existing == null || existing == packageVersion) {
                "Conflicting central package version for '${packageVersion.name}' in solution '$solutionName'."
            }
            merged.putIfAbsent(packageVersion.name, packageVersion)
        }
        return merged.values.sortedBy(DotnetPackageVersion::name)
    }
}
