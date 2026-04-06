package io.github.lmliam.microsmith.artifact.services.dotnet.asp

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import io.github.lmliam.microsmith.artifact.core.ArtifactContributor
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.DotnetAspWorkspace

@ServiceProvider(ArtifactContributor::class)
class DotnetAspArtifactContributor : ArtifactContributor<DotnetAspWorkspace> {
    override val resolvedType = DotnetAspWorkspace::class

    override fun contribute(model: DotnetAspWorkspace): List<ArtifactContribution<*>> =
        model.servicesByName.values.sortedBy {
            it.name
        }.mapIndexed { index, service ->
            val httpPort = BASE_HTTP_PORT + (index * PORT_STRIDE)
            DotnetAspServiceContribution(
                artifactId = DotnetAspServiceArtifactId(service.solutionName, service.projectName),
                serviceName = service.name,
                targetFrameworkMoniker = service.targetFrameworkMoniker,
                outputRoot = service.outputRoot,
                httpPort = httpPort,
                httpsPort = httpPort + 1,
            )
        }

    private companion object {
        const val BASE_HTTP_PORT = 5000
        const val PORT_STRIDE = 10
    }
}
