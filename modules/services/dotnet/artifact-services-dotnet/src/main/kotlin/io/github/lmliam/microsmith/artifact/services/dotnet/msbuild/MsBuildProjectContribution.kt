package io.github.lmliam.microsmith.artifact.services.dotnet.msbuild

import io.github.lmliam.microsmith.artifact.ArtifactContribution

public data class MsBuildProjectContribution(
    override val artifactId: MsBuildProjectArtifactId,
    public val projectAttributes: Map<String, String> = emptyMap(),
    public val properties: Map<String, String> = emptyMap(),
    public val items: List<MsBuildItem> = emptyList(),
) : ArtifactContribution<MsBuildProjectArtifact> {
    init {
        projectAttributes.keys.forEach(::requireAttributeName)
        properties.keys.forEach(::requirePropertyName)
    }
}
