package io.github.lmliam.microsmith.artifact.services.dotnet.msbuild

import io.github.lmliam.microsmith.artifact.services.dotnet.core.DotnetArtifact

data class MsBuildProjectArtifact(
    override val id: MsBuildProjectArtifactId,
    val projectAttributes: Map<String, String> = emptyMap(),
    val properties: Map<String, String>,
    val items: List<MsBuildItem>,
    val origins: Set<String> = emptySet(),
) : DotnetArtifact {
    init {
        projectAttributes.keys.forEach(MsBuildNames::requireAttributeName)
        properties.keys.forEach(MsBuildNames::requirePropertyName)
    }
}
