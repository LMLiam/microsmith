package io.github.lmliam.microsmith.artifact.services.dotnet.packages.references

sealed interface DotnetPackageReferenceVersion {
    data object Central : DotnetPackageReferenceVersion

    data class Explicit(val version: String) : DotnetPackageReferenceVersion
}
