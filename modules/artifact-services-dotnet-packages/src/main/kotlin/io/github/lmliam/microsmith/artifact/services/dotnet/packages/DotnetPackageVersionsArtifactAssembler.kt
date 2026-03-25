package io.github.lmliam.microsmith.artifact.services.dotnet.packages

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.ArtifactAssembler
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import kotlin.reflect.KClass

@ServiceProvider(ArtifactAssembler::class)
class DotnetPackageVersionsArtifactAssembler : ArtifactAssembler<DotnetPackageVersionsArtifact> {
    override val artifactType: KClass<DotnetPackageVersionsArtifact> = DotnetPackageVersionsArtifact::class

    override fun create(first: ArtifactContribution<DotnetPackageVersionsArtifact>): DotnetPackageVersionsArtifact {
        val contribution = requireContribution(first)
        return DotnetPackageVersionsArtifact(
            id = contribution.artifactId,
            packages = linkedMapOf<String, String>().apply { putAll(contribution.packages) },
        )
    }

    override fun merge(
        current: DotnetPackageVersionsArtifact,
        contribution: ArtifactContribution<DotnetPackageVersionsArtifact>,
    ): DotnetPackageVersionsArtifact {
        val next = requireContribution(contribution)
        val merged = linkedMapOf<String, String>().apply { putAll(current.packages) }
        next.packages.forEach { (packageName, version) ->
            val existing = merged[packageName]
            require(existing == null || existing == version) {
                "Conflicting central package version for '$packageName' in solution '${current.id.solutionName}'."
            }
            merged[packageName] = version
        }
        return current.copy(packages = merged)
    }

    private fun requireContribution(
        contribution: ArtifactContribution<DotnetPackageVersionsArtifact>,
    ): DotnetPackageVersionsContribution {
        require(contribution is DotnetPackageVersionsContribution) {
            "Unsupported dotnet package versions contribution type: ${contribution::class}"
        }
        return contribution
    }
}
