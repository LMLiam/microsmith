package io.github.lmliam.microsmith.artifact.services.dotnet.msbuild

import io.github.lmliam.microsmith.artifact.ArtifactId
import kotlin.reflect.KClass

public sealed interface MsBuildProjectArtifactId : ArtifactId<MsBuildProjectArtifact> {
    public val solutionName: String

    override val artifactType: KClass<MsBuildProjectArtifact>
        get() = MsBuildProjectArtifact::class

    public data class DirectoryPackagesProps(override val solutionName: String) : MsBuildProjectArtifactId

    public data class DirectoryBuildProps(override val solutionName: String, public val projectName: String) :
        MsBuildProjectArtifactId

    public data class Project(override val solutionName: String, public val projectName: String) :
        MsBuildProjectArtifactId
}
