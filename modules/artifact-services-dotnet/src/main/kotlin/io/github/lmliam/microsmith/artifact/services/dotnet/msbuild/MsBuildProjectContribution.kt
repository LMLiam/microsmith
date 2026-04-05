package io.github.lmliam.microsmith.artifact.services.dotnet.msbuild

import io.github.lmliam.microsmith.artifact.core.ArtifactContribution

data class MsBuildProjectContribution(
    override val artifactId: MsBuildProjectArtifactId,
    val projectAttributes: Map<String, String> = emptyMap(),
    val properties: Map<String, String> = emptyMap(),
    val items: List<MsBuildItem> = emptyList(),
) : ArtifactContribution<MsBuildProjectArtifact> {
    init {
        projectAttributes.keys.forEach(MsBuildNames::requireAttributeName)
        properties.keys.forEach(MsBuildNames::requirePropertyName)
    }
}
