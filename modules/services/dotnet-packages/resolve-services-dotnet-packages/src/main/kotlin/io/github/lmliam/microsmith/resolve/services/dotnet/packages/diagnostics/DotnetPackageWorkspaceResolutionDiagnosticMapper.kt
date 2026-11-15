package io.github.lmliam.microsmith.resolve.services.dotnet.packages.diagnostics

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionIssueDiagnosticMapper
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.DotnetPackagesPluginScope

@Inject
@ContributesIntoSet(DotnetPackagesPluginScope::class, binding = binding<ResolutionIssueDiagnosticMapper<*>>())
internal class DotnetPackageWorkspaceResolutionDiagnosticMapper :
    ResolutionIssueDiagnosticMapper<DotnetPackageWorkspaceResolutionIssue> {
    override val issueType = DotnetPackageWorkspaceResolutionIssue::class

    override fun map(issue: DotnetPackageWorkspaceResolutionIssue): ResolutionDiagnostic =
        when (issue) {
            is DotnetPackageWorkspaceResolutionIssue.PackageNotCentrallyOwned ->
                ResolutionDiagnostic(
                    code = "dotnet.packages.package-not-centrally-owned",
                    message =
                        "Dotnet service '${issue.serviceName}' references package '${issue.packageName}' but " +
                            "solution " +
                            "'${issue.solutionName}' does not centrally own it.",
                )

            is DotnetPackageWorkspaceResolutionIssue.PackageVersionRequired ->
                ResolutionDiagnostic(
                    code = "dotnet.packages.package-version-required",
                    message =
                        "Dotnet service '${issue.serviceName}' references package '${issue.packageName}' without a " +
                            "version and without central package ownership.",
                )

            is DotnetPackageWorkspaceResolutionIssue.MixedPackageVersionManagement ->
                ResolutionDiagnostic(
                    code = "dotnet.packages.mixed-version-management",
                    message =
                        "Dotnet service '${issue.serviceName}' declares package '${issue.packageName}' with an " +
                            "explicit version but solution '${issue.solutionName}' uses central package management. " +
                            "Mixed central and direct package version management is not supported " +
                            "within the same solution.",
                )
        }
}
