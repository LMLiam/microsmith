package thirdparty.common

import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.ArtifactId

data object StageArtifactId : ArtifactId<StageArtifact> {
    override val artifactType = StageArtifact::class
}

data class StageArtifact(val text: String) : Artifact {
    override val id = StageArtifactId
}

data class StageArtifactContribution(val text: String) : ArtifactContribution<StageArtifact> {
    override val artifactId = StageArtifactId
}
