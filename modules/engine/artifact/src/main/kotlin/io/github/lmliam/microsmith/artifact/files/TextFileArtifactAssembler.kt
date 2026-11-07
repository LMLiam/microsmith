package io.github.lmliam.microsmith.artifact.files

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssembler

@ServiceProvider(ArtifactAssembler::class)
class TextFileArtifactAssembler : ArtifactAssembler<TextFileArtifact> {
    override val artifactType = TextFileArtifact::class

    override fun create(contribution: ArtifactContribution<TextFileArtifact>): TextFileArtifact {
        val textContribution = contribution.requireTextContribution()
        return TextFileArtifact(
            id = textContribution.artifactId,
            contents = textContribution.contents,
        )
    }

    override fun merge(
        current: TextFileArtifact,
        contribution: ArtifactContribution<TextFileArtifact>,
    ): TextFileArtifact {
        val nextContribution = contribution.requireTextContribution()
        require(current.contents == nextContribution.contents) {
            "Conflicting text artifact contributions for '${current.id.relativePath}' " +
                "under '${current.id.outputRoot}'."
        }
        return current
    }

    private fun ArtifactContribution<TextFileArtifact>.requireTextContribution(): TextFileArtifactContribution {
        require(this is TextFileArtifactContribution) {
            "Unsupported text artifact contribution type '${this::class.qualifiedName ?: this::class.simpleName}'."
        }
        return this
    }
}
