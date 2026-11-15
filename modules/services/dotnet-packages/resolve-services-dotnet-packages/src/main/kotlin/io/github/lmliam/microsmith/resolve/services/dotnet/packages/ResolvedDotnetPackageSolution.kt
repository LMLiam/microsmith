package io.github.lmliam.microsmith.resolve.services.dotnet.packages

public data class ResolvedDotnetPackageSolution(
    public val name: String,
    public val packages: List<ResolvedDotnetPackageVersion>,
) {
    internal fun packageVersionsByName(): Map<String, ResolvedDotnetPackageVersion> =
        packages.associateBy(ResolvedDotnetPackageVersion::name)
}
