package io.github.lmliam.microsmith.artifact.services.dotnet.msbuild

data class MsBuildItem(
    val itemName: String,
    val include: String,
    val attributes: Map<String, String> = emptyMap(),
) {
    init {
        MsBuildNames.requireItemName(itemName)
        attributes.keys.forEach(MsBuildNames::requireAttributeName)
    }
}
