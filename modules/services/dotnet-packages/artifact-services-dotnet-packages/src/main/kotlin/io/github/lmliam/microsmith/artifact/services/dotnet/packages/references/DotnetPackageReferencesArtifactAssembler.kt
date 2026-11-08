package io.github.lmliam.microsmith.artifact.services.dotnet.packages.references

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssembler

@ServiceProvider(ArtifactAssembler::class)
class DotnetPackageReferencesArtifactAssembler : ArtifactAssembler<DotnetPackageReferencesArtifact> {
    override val artifactType = DotnetPackageReferencesArtifact::class

    override fun create(
        contribution: ArtifactContribution<DotnetPackageReferencesArtifact>
    ): DotnetPackageReferencesArtifact {
        val referencesContribution = requireContribution(contribution)
        return DotnetPackageReferencesArtifact(
            id = referencesContribution.artifactId,
            solutionName = referencesContribution.solutionName,
            projectName = referencesContribution.projectName,
            packages =
                mergePackages(
                    emptyList(),
                    referencesContribution.packages,
                    referencesContribution.artifactId.serviceName,
                ),
        )
    }

    override fun merge(
        current: DotnetPackageReferencesArtifact,
        contribution: ArtifactContribution<DotnetPackageReferencesArtifact>,
    ): DotnetPackageReferencesArtifact {
        val next = requireContribution(contribution)
        require(current.solutionName == next.solutionName) {
            "Conflicting dotnet solution for package references owned by service '${current.id.serviceName}'."
        }
        require(current.projectName == next.projectName) {
            "Conflicting dotnet project for package references owned by service '${current.id.serviceName}'."
        }
        return current.copy(packages = mergePackages(current.packages, next.packages, current.id.serviceName))
    }

    private fun requireContribution(
        contribution: ArtifactContribution<DotnetPackageReferencesArtifact>
    ): DotnetPackageReferencesContribution {
        require(contribution is DotnetPackageReferencesContribution) {
            "Unsupported dotnet package references contribution type: ${contribution::class}"
        }
        return contribution
    }

    private fun mergePackages(
        current: List<DotnetPackageReference>,
        next: List<DotnetPackageReference>,
        serviceName: String,
    ): List<DotnetPackageReference> {
        val merged = LinkedHashMap(current.associateBy(DotnetPackageReference::name))
        next.forEach { packageReference ->
            val existing = merged[packageReference.name]
            require(existing == null || existing == packageReference) {
                "Conflicting dotnet package reference '${packageReference.name}' for service '$serviceName'."
            }
            merged.putIfAbsent(packageReference.name, packageReference)
        }
        return merged.values.sortedBy(DotnetPackageReference::name)
    }
}
