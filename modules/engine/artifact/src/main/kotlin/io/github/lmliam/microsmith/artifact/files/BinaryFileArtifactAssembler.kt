package io.github.lmliam.microsmith.artifact.files

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssembler

@ServiceProvider(ArtifactAssembler::class)
class BinaryFileArtifactAssembler : ArtifactAssembler<BinaryFileArtifact> {
    override val artifactType = BinaryFileArtifact::class

    override fun create(contribution: ArtifactContribution<BinaryFileArtifact>): BinaryFileArtifact {
        val binaryContribution = contribution.requireBinaryContribution()
        return BinaryFileArtifact(
            id = binaryContribution.artifactId,
            contents = binaryContribution.contents,
        )
    }

    override fun merge(
        current: BinaryFileArtifact,
        contribution: ArtifactContribution<BinaryFileArtifact>,
    ): BinaryFileArtifact {
        val nextContribution = contribution.requireBinaryContribution()
        require(current.contents.contentEquals(nextContribution.contents)) {
            "Conflicting binary artifact contributions for '${current.id.relativePath}' " +
                "under '${current.id.outputRoot}'."
        }
        return current
    }

    private fun ArtifactContribution<BinaryFileArtifact>.requireBinaryContribution(): BinaryFileArtifactContribution {
        require(this is BinaryFileArtifactContribution) {
            "Unsupported binary artifact contribution type '${this::class.qualifiedName ?: this::class.simpleName}'."
        }
        return this
    }
}
