package io.github.lmliam.microsmith.artifact.services.dotnet.msbuild

data class MsBuildItem(
    val type: String,
    val include: String,
    val metadata: Map<String, String> = emptyMap(),
)
