package io.github.lmliam.microsmith.artifact.services.dotnet.msbuild

import io.github.lmliam.microsmith.artifact.core.ArtifactId

data class MsBuildProjectArtifactId(
    val solutionName: String,
    val projectName: String? = null,
    val kind: MsBuildProjectKind,
) : ArtifactId<MsBuildProjectArtifact> {
    override val artifactType = MsBuildProjectArtifact::class
}
