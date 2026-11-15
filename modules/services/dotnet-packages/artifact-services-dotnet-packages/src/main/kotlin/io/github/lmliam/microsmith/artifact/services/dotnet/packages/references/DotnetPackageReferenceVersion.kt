package io.github.lmliam.microsmith.artifact.services.dotnet.packages.references

public sealed interface DotnetPackageReferenceVersion {
    public data object Central : DotnetPackageReferenceVersion

    public data class Explicit(public val version: String) : DotnetPackageReferenceVersion
}
