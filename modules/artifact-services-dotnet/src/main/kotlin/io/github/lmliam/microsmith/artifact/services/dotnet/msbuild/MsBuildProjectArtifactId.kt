package io.github.lmliam.microsmith.artifact.services.dotnet.msbuild

import io.github.lmliam.microsmith.artifact.core.ArtifactId
import kotlin.reflect.KClass

data class MsBuildProjectArtifactId(
    val solutionName: String,
    val projectName: String? = null,
    val kind: MsBuildProjectKind,
) : ArtifactId<MsBuildProjectArtifact> {
    override val artifactType: KClass<MsBuildProjectArtifact> = MsBuildProjectArtifact::class
}
