package io.github.lmliam.microsmith.artifact.services.dotnet.msbuild

import io.github.lmliam.microsmith.artifact.services.dotnet.DotnetArtifact

public data class MsBuildProjectArtifact(
    override val id: MsBuildProjectArtifactId,
    public val projectAttributes: Map<String, String> = emptyMap(),
    public val properties: Map<String, String>,
    public val items: List<MsBuildItem>,
) : DotnetArtifact {
    init {
        projectAttributes.keys.forEach(::requireAttributeName)
        properties.keys.forEach(::requirePropertyName)
    }
}
