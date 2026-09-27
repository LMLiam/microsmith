package io.github.lmliam.microsmith.resolve.services.dotnet.packages.diagnostics
import io.github.lmliam.microsmith.resolve.services.dotnet.DotnetResolutionIssue

sealed interface DotnetPackageWorkspaceResolutionIssue : DotnetResolutionIssue {
    data class PackageNotCentrallyOwned(val serviceName: String, val solutionName: String, val packageName: String) :
        DotnetPackageWorkspaceResolutionIssue

    data class PackageVersionRequired(val serviceName: String, val packageName: String) :
        DotnetPackageWorkspaceResolutionIssue

    data class MixedPackageVersionManagement(
        val serviceName: String,
        val solutionName: String,
        val packageName: String,
    ) : DotnetPackageWorkspaceResolutionIssue
}
