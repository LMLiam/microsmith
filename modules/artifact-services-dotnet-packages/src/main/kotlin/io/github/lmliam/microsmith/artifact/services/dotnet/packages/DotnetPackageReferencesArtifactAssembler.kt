package io.github.lmliam.microsmith.artifact.services.dotnet.packages

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.ArtifactAssembler
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import kotlin.reflect.KClass

@ServiceProvider(ArtifactAssembler::class)
class DotnetPackageReferencesArtifactAssembler : ArtifactAssembler<DotnetPackageReferencesArtifact> {
    override val artifactType: KClass<DotnetPackageReferencesArtifact> = DotnetPackageReferencesArtifact::class

    override fun create(first: ArtifactContribution<DotnetPackageReferencesArtifact>): DotnetPackageReferencesArtifact {
        val contribution = requireContribution(first)
        return DotnetPackageReferencesArtifact(
            id = contribution.artifactId,
            solutionName = contribution.solutionName,
            projectName = contribution.projectName,
            packages = contribution.packages.distinct().sorted(),
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
        return current.copy(packages = (current.packages + next.packages).distinct().sorted())
    }

    private fun requireContribution(
        contribution: ArtifactContribution<DotnetPackageReferencesArtifact>,
    ): DotnetPackageReferencesContribution {
        require(contribution is DotnetPackageReferencesContribution) {
            "Unsupported dotnet package references contribution type: ${contribution::class}"
        }
        return contribution
    }
}
