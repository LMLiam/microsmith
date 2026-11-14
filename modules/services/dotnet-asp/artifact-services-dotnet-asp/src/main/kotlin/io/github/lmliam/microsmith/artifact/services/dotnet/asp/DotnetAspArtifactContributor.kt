package io.github.lmliam.microsmith.artifact.services.dotnet.asp

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.ArtifactContributor
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.service.DotnetAspServiceArtifactFactory
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.service.DotnetAspServiceArtifactId
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution.DotnetAspWorkspace

@ServiceProvider(ArtifactContributor::class)
class DotnetAspArtifactContributor : ArtifactContributor<DotnetAspWorkspace> {
    override val resolvedType = DotnetAspWorkspace::class

    override fun contribute(model: DotnetAspWorkspace): List<ArtifactContribution<*>> =
        model.servicesByName.values
            .map { service -> service to DotnetAspServiceArtifactId(service.solutionName, service.projectName) }
            .sortedWith(
                compareBy(
                    { (_, artifactId) -> artifactId.solutionName },
                    { (_, artifactId) -> artifactId.projectName },
                )
            )
            .map { (service, artifactId) -> DotnetAspServiceArtifactFactory(service, artifactId).createContribution() }
}
