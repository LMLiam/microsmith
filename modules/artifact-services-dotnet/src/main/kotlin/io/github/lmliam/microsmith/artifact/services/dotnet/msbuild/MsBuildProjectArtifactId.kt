package io.github.lmliam.microsmith.artifact.services.dotnet.msbuild

import io.github.lmliam.microsmith.artifact.core.ArtifactId
import java.nio.file.Path
import kotlin.reflect.KClass

private val defaultMsBuildOutputRoot: Path = Path.of(".")

data class MsBuildProjectArtifactId(
    val relativePath: Path,
    val outputRoot: Path = defaultMsBuildOutputRoot,
) : ArtifactId<MsBuildProjectArtifact> {
    override val artifactType: KClass<MsBuildProjectArtifact> = MsBuildProjectArtifact::class
}
