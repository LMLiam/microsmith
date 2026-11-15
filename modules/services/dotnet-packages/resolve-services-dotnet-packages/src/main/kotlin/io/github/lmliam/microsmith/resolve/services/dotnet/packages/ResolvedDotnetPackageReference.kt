package io.github.lmliam.microsmith.resolve.services.dotnet.packages

public data class ResolvedDotnetPackageReference(
    public val name: String,
    public val version: ResolvedDotnetPackageReferenceVersion,
)
