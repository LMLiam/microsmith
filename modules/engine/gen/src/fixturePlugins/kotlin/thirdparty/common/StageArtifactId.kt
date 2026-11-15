package thirdparty.common

import io.github.lmliam.microsmith.artifact.ArtifactId

data object StageArtifactId : ArtifactId<StageArtifact> {
    override val artifactType = StageArtifact::class
}
