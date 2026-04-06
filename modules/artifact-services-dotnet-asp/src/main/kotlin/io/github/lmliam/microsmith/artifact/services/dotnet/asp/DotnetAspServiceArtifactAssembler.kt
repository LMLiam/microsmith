package io.github.lmliam.microsmith.artifact.services.dotnet.asp

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.ArtifactAssembler
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution

@ServiceProvider(ArtifactAssembler::class)
class DotnetAspServiceArtifactAssembler : ArtifactAssembler<DotnetAspServiceArtifact> {
    override val artifactType = DotnetAspServiceArtifact::class

    override fun create(first: ArtifactContribution<DotnetAspServiceArtifact>): DotnetAspServiceArtifact {
        val contribution = requireContribution(first)
        return DotnetAspServiceArtifact(
            id = contribution.artifactId,
            serviceName = contribution.serviceName,
            targetFrameworkMoniker = contribution.targetFrameworkMoniker,
            outputRoot = contribution.outputRoot,
            httpPort = contribution.httpPort,
            httpsPort = contribution.httpsPort,
            models = contribution.models,
            rest = contribution.rest,
        )
    }

    override fun merge(
        current: DotnetAspServiceArtifact,
        contribution: ArtifactContribution<DotnetAspServiceArtifact>,
    ): DotnetAspServiceArtifact {
        val next = requireContribution(contribution)
        require(current == create(next)) {
            "Conflicting ASP.NET scaffold contributions for solution '${current.id.solutionName}' " +
                "project '${current.id.projectName}'."
        }
        return current
    }

    private fun requireContribution(
        contribution: ArtifactContribution<DotnetAspServiceArtifact>,
    ): DotnetAspServiceContribution {
        require(contribution is DotnetAspServiceContribution) {
            "Unsupported ASP.NET service contribution type: ${contribution::class}"
        }
        return contribution
    }
}
