package io.github.lmliam.microsmith.artifact.services.dotnet.msbuild

public class MsBuildItem(
    public val itemName: String,
    public val include: String,
    attributes: Map<String, String> = emptyMap(),
) {
    public val attributes: Map<String, String> = attributes.toMap()

    init {
        requireItemName(itemName)
        this.attributes.keys.forEach(::requireAttributeName)
    }

    override fun equals(other: Any?): Boolean =
        other is MsBuildItem && itemName == other.itemName && include == other.include && attributes == other.attributes

    override fun hashCode(): Int {
        var result = itemName.hashCode()
        result = 31 * result + include.hashCode()
        result = 31 * result + attributes.hashCode()
        return result
    }

    override fun toString(): String = "MsBuildItem(itemName=$itemName, include=$include, attributes=$attributes)"
}
