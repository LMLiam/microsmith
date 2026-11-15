package thirdparty.common

import io.github.lmliam.microsmith.artifact.Artifact

data class StageArtifact(val text: String) : Artifact {
    override val id = StageArtifactId
}
