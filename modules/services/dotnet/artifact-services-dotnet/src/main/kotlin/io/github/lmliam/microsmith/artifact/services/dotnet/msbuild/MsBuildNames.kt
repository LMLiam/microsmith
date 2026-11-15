package io.github.lmliam.microsmith.artifact.services.dotnet.msbuild

public object MsBuildNames {
    public const val SDK_ATTRIBUTE: String = "Sdk"
    public const val IMPLICIT_USINGS_PROPERTY: String = "ImplicitUsings"
    public const val MANAGE_PACKAGE_VERSIONS_CENTRALLY_PROPERTY: String = "ManagePackageVersionsCentrally"
    public const val NULLABLE_PROPERTY: String = "Nullable"
    public const val PACKAGE_REFERENCE_ITEM: String = "PackageReference"
    public const val PACKAGE_VERSION_ITEM: String = "PackageVersion"
    public const val TARGET_FRAMEWORK_PROPERTY: String = "TargetFramework"
    public const val VERSION_ATTRIBUTE: String = "Version"
}

internal fun requirePropertyName(value: String): String {
    require(value.isNotBlank()) { "MSBuild property name cannot be blank." }
    require(value.none(Char::isWhitespace)) { "MSBuild property name cannot contain whitespace: '$value'." }
    return value
}

internal fun requireAttributeName(value: String): String {
    require(value.isNotBlank()) { "MSBuild attribute name cannot be blank." }
    require(value.none(Char::isWhitespace)) { "MSBuild attribute name cannot contain whitespace: '$value'." }
    return value
}

internal fun requireItemName(value: String): String {
    require(value.isNotBlank()) { "MSBuild item name cannot be blank." }
    return value
}
