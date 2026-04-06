package io.github.lmliam.microsmith.artifact.services.dotnet.asp

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import io.github.lmliam.microsmith.artifact.core.ArtifactContributor
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.DotnetAspWorkspace

@ServiceProvider(ArtifactContributor::class)
class DotnetAspArtifactContributor : ArtifactContributor<DotnetAspWorkspace> {
    override val resolvedType = DotnetAspWorkspace::class

    override fun contribute(model: DotnetAspWorkspace): List<ArtifactContribution<*>> {
        return model.servicesByName.values
            .map { service ->
                service to DotnetAspServiceArtifactId(service.solutionName, service.projectName)
            }.sortedWith(
                compareBy(
                    { (_, artifactId) -> artifactId.solutionName },
                    { (_, artifactId) -> artifactId.projectName },
                ),
            )
            .also { serviceArtifacts ->
                validateUniqueDotnetAspPorts(serviceArtifacts.map { (_, artifactId) -> artifactId })
            }.map { (service, artifactId) ->
                val httpPort = dotnetAspHttpPortFor(artifactId)
                DotnetAspServiceContribution(
                    artifactId = artifactId,
                    serviceName = service.name,
                    targetFrameworkMoniker = service.targetFrameworkMoniker,
                    outputRoot = service.outputRoot,
                    httpPort = httpPort,
                    httpsPort = httpPort + 1,
                    models = service.models,
                    rest = service.rest,
                )
            }
    }
}
