package io.github.lmliam.microsmith.artifact.services.dotnet.asp.service

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssembler

@ServiceProvider(ArtifactAssembler::class)
class DotnetAspServiceArtifactAssembler : ArtifactAssembler<DotnetAspServiceArtifact> {
    override val artifactType = DotnetAspServiceArtifact::class

    override fun create(contribution: ArtifactContribution<DotnetAspServiceArtifact>): DotnetAspServiceArtifact {
        val serviceContribution = requireContribution(contribution)
        return DotnetAspServiceArtifactMapper.map(serviceContribution)
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
