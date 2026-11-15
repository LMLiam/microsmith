package io.github.lmliam.microsmith.resolve.services.dotnet.packages

public data class ResolvedDotnetPackageService(
    public val name: String,
    public val solution: String,
    public val project: String,
    public val packages: List<ResolvedDotnetPackageReference>,
)
