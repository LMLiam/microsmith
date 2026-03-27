package io.github.lmliam.microsmith.artifact.services.dotnet.msbuild

data class MsBuildItem(
    val itemName: String,
    val include: String,
    val attributes: Map<String, String> = emptyMap(),
)
