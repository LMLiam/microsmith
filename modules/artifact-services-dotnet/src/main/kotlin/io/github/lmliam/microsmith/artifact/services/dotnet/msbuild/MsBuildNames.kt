package io.github.lmliam.microsmith.artifact.services.dotnet.msbuild

object MsBuildNames {
    const val MANAGE_PACKAGE_VERSIONS_CENTRALLY_PROPERTY = "ManagePackageVersionsCentrally"
    const val PACKAGE_REFERENCE_ITEM = "PackageReference"
    const val PACKAGE_VERSION_ITEM = "PackageVersion"
    const val VERSION_ATTRIBUTE = "Version"

    fun requirePropertyName(value: String): String {
        require(value.isNotBlank()) { "MSBuild property name cannot be blank." }
        require(value.none(Char::isWhitespace)) { "MSBuild property name cannot contain whitespace: '$value'." }
        return value
    }

    fun requireAttributeName(value: String): String {
        require(value.isNotBlank()) { "MSBuild attribute name cannot be blank." }
        require(value.none(Char::isWhitespace)) { "MSBuild attribute name cannot contain whitespace: '$value'." }
        return value
    }

    fun requireItemName(value: String): String {
        require(value.isNotBlank()) { "MSBuild item name cannot be blank." }
        return value
    }
}
