package io.github.lmliam.microsmith.artifact.services.dotnet.msbuild

class MsBuildItem(
    val itemName: String,
    val include: String,
    attributes: Map<String, String> = emptyMap(),
) {
    val attributes: Map<String, String> = attributes.toMap()

    init {
        MsBuildNames.requireItemName(itemName)
        this.attributes.keys.forEach(MsBuildNames::requireAttributeName)
    }

    override fun equals(other: Any?): Boolean {
        return other is MsBuildItem &&
            itemName == other.itemName &&
            include == other.include &&
            attributes == other.attributes
    }

    override fun hashCode(): Int {
        var result = itemName.hashCode()
        result = 31 * result + include.hashCode()
        result = 31 * result + attributes.hashCode()
        return result
    }

    override fun toString(): String {
        return "MsBuildItem(itemName=$itemName, include=$include, attributes=$attributes)"
    }
}
