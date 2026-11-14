package io.github.lmliam.microsmith.artifact.services.dotnet.msbuild

import io.github.lmliam.microsmith.artifact.ArtifactId

sealed interface MsBuildProjectArtifactId : ArtifactId<MsBuildProjectArtifact> {
    val solutionName: String

    override val artifactType
        get() = MsBuildProjectArtifact::class

    data class DirectoryPackagesProps(override val solutionName: String) : MsBuildProjectArtifactId

    data class DirectoryBuildProps(override val solutionName: String, val projectName: String) :
        MsBuildProjectArtifactId

    data class Project(override val solutionName: String, val projectName: String) : MsBuildProjectArtifactId
}
