package io.github.lmliam.microsmith.resolve.services.dotnet.packages

public sealed interface ResolvedDotnetPackageReferenceVersion {
    public data object Central : ResolvedDotnetPackageReferenceVersion

    public data class Explicit(public val version: String) : ResolvedDotnetPackageReferenceVersion
}
