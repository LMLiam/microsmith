package io.github.lmliam.microsmith.artifact.services.dotnet.msbuild

enum class MsBuildProjectKind(
    val fileName: String,
) {
    DirectoryPackagesProps("Directory.Packages.props"),
    PackageReferencesProps("PackageReferences.props"),
}
