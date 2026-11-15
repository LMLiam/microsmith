package io.github.lmliam.microsmith.artifact.services.dotnet.packages.versions

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssembler
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.DotnetPackagesPluginScope

@Inject
@ContributesIntoSet(DotnetPackagesPluginScope::class, binding = binding<ArtifactAssembler<*>>())
internal class DotnetPackageVersionsArtifactAssembler : ArtifactAssembler<DotnetPackageVersionsArtifact> {
    override val artifactType = DotnetPackageVersionsArtifact::class

    override fun create(
        contribution: ArtifactContribution<DotnetPackageVersionsArtifact>,
    ): DotnetPackageVersionsArtifact {
        val versionsContribution = requireContribution(contribution)
        return DotnetPackageVersionsArtifact(
            id = versionsContribution.artifactId,
            packages =
                mergePackages(emptyList(), versionsContribution.packages, versionsContribution.artifactId.solutionName),
        )
    }

    override fun merge(
        current: DotnetPackageVersionsArtifact,
        contribution: ArtifactContribution<DotnetPackageVersionsArtifact>,
    ): DotnetPackageVersionsArtifact {
        val next = requireContribution(contribution)
        return current.copy(packages = mergePackages(current.packages, next.packages, current.id.solutionName))
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
