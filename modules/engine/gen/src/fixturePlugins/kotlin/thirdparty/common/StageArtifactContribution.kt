package thirdparty.common

import io.github.lmliam.microsmith.artifact.ArtifactContribution

data class StageArtifactContribution(val text: String) : ArtifactContribution<StageArtifact> {
    override val artifactId = StageArtifactId
}
