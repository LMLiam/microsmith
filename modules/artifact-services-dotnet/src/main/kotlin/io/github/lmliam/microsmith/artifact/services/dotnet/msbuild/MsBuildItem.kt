package io.github.lmliam.microsmith.artifact.services.dotnet.msbuild

data class MsBuildItem(
    val itemName: MsBuildItemName,
    val include: String,
    val attributes: Map<MsBuildAttributeName, String> = emptyMap(),
)
