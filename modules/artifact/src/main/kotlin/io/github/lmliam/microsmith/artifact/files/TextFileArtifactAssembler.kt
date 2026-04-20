package io.github.lmliam.microsmith.artifact.files

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.ArtifactAssembler
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution

@ServiceProvider(ArtifactAssembler::class)
class TextFileArtifactAssembler : ArtifactAssembler<TextFileArtifact> {
    override val artifactType = TextFileArtifact::class

    override fun create(first: ArtifactContribution<TextFileArtifact>): TextFileArtifact {
        val contribution = requireContribution(first)
        return TextFileArtifact(
            id = contribution.artifactId,
            contents = contribution.contents,
            origins = contribution.origins,
        )
    }

    override fun merge(
        current: TextFileArtifact,
        contribution: ArtifactContribution<TextFileArtifact>,
    ): TextFileArtifact {
        val next = requireContribution(contribution)
        require(current.contents == next.contents) {
            "Conflicting text artifact contributions for '${current.id.relativePath}' " +
                "under '${current.id.outputRoot}'."
        }
        if (current.origins == next.origins) {
            return current
        }
        return current.copy(origins = current.origins + next.origins)
    }

    private fun requireContribution(
        contribution: ArtifactContribution<TextFileArtifact>,
    ): TextFileArtifactContribution {
        require(contribution is TextFileArtifactContribution) {
            "Unsupported text artifact contribution type: ${contribution::class}"
        }
        return contribution
    }
}
