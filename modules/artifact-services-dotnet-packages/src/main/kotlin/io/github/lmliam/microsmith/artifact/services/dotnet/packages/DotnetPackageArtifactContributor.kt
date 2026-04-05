package io.github.lmliam.microsmith.artifact.services.dotnet.packages

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import io.github.lmliam.microsmith.artifact.core.ArtifactContributor
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.DotnetPackageWorkspace

@ServiceProvider(ArtifactContributor::class)
class DotnetPackageArtifactContributor : ArtifactContributor<DotnetPackageWorkspace> {
    override val resolvedType = DotnetPackageWorkspace::class

    override fun contribute(model: DotnetPackageWorkspace): List<ArtifactContribution<*>> {
        val solutionContributions = model.solutionsByName.values.sortedBy { it.name }.map { solution ->
            DotnetPackageVersionsContribution(
                artifactId = DotnetPackageVersionsArtifactId(solution.name),
                packages = solution.packages.map { packageVersion ->
                    DotnetPackageVersion(
                        name = packageVersion.name,
                        version = packageVersion.version,
                    )
                },
            )
        }

        val serviceContributions = model.servicesByName.values.sortedBy { it.name }.map { service ->
            DotnetPackageReferencesContribution(
                artifactId = DotnetPackageReferencesArtifactId(service.name),
                solutionName = service.solution,
                projectName = service.project,
                packages = service.packages.map { packageReference ->
                    DotnetPackageReference(
                        name = packageReference.name,
                        version = packageReference.version,
                    )
                },
            )
        }

        return solutionContributions + serviceContributions
    }
}
