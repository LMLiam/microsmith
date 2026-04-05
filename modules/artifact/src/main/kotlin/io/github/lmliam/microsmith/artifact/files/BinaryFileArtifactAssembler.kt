package io.github.lmliam.microsmith.artifact.files

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.ArtifactAssembler
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution

@ServiceProvider(ArtifactAssembler::class)
class BinaryFileArtifactAssembler : ArtifactAssembler<BinaryFileArtifact> {
    override val artifactType = BinaryFileArtifact::class

    override fun create(first: ArtifactContribution<BinaryFileArtifact>): BinaryFileArtifact {
        val contribution = requireContribution(first)
        return BinaryFileArtifact(
            id = contribution.artifactId,
            contents = contribution.copiedContents,
        )
    }

    override fun merge(
        current: BinaryFileArtifact,
        contribution: ArtifactContribution<BinaryFileArtifact>,
    ): BinaryFileArtifact {
        val next = requireContribution(contribution)
        require(current.copiedContents.contentEquals(next.copiedContents)) {
            "Conflicting binary artifact contributions for '${current.id.relativePath}' " +
                "under '${current.id.outputRoot}'."
        }
        return current
    }

    private fun requireContribution(
        contribution: ArtifactContribution<BinaryFileArtifact>,
    ): BinaryFileArtifactContribution {
        require(contribution is BinaryFileArtifactContribution) {
            "Unsupported binary artifact contribution type: ${contribution::class}"
        }
        return contribution
    }
}
