package io.github.lmliam.microsmith.artifact.services.dotnet.msbuild

import io.github.lmliam.microsmith.artifact.services.dotnet.core.DotnetArtifact

data class MsBuildProjectArtifact(
    override val id: MsBuildProjectArtifactId,
    val properties: Map<MsBuildPropertyName, String>,
    val items: List<MsBuildItem>,
) : DotnetArtifact
