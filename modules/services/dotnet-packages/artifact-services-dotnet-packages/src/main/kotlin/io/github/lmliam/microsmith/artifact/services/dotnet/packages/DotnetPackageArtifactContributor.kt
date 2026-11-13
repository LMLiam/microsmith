package io.github.lmliam.microsmith.artifact.services.dotnet.packages

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.ArtifactContributor
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.resolution.DotnetPackageWorkspace

@ServiceProvider(ArtifactContributor::class)
class DotnetPackageArtifactContributor : ArtifactContributor<DotnetPackageWorkspace> {
    override val resolvedType = DotnetPackageWorkspace::class

    override fun contribute(model: DotnetPackageWorkspace): List<ArtifactContribution<*>> {
        val solutionContributions =
            model.solutionsByName.values.sortedBy { it.name }.map(DotnetPackageVersionsContributionMapper::map)

        val serviceContributions =
            model.servicesByName.values.sortedBy { it.name }.map(DotnetPackageReferencesContributionMapper::map)

        return solutionContributions + serviceContributions
    }
}
