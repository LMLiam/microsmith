package io.github.lmliam.microsmith.resolve.services.dotnet.packages

sealed interface ResolvedDotnetPackageReferenceVersion {
    data object Central : ResolvedDotnetPackageReferenceVersion

    data class Explicit(val version: String) : ResolvedDotnetPackageReferenceVersion
}
