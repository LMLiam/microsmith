package io.github.lmliam.microsmith.resolve.services.dotnet.diagnostics

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionIssueDiagnosticMapper
import io.github.lmliam.microsmith.resolve.services.dotnet.DotnetWorkspaceResolutionIssue

@ServiceProvider(ResolutionIssueDiagnosticMapper::class)
class DotnetWorkspaceResolutionDiagnosticMapper : ResolutionIssueDiagnosticMapper<DotnetWorkspaceResolutionIssue> {
    override val issueType = DotnetWorkspaceResolutionIssue::class

    override fun map(issue: DotnetWorkspaceResolutionIssue): ResolutionDiagnostic = when (issue) {
        is DotnetWorkspaceResolutionIssue.TargetNotConfigured -> ResolutionDiagnostic(
            code = "dotnet.workspace.target-not-configured",
            message = "Dotnet target not configured for service '${issue.serviceName}'.",
        )

        is DotnetWorkspaceResolutionIssue.SolutionNotConfigured -> ResolutionDiagnostic(
            code = "dotnet.workspace.solution-not-configured",
            message = "Dotnet solution not configured for service '${issue.serviceName}'.",
        )

        is DotnetWorkspaceResolutionIssue.SolutionNotDeclared -> ResolutionDiagnostic(
            code = "dotnet.workspace.solution-not-declared",
            message = "Dotnet solution '${issue.solutionName}' is not declared for service '${issue.serviceName}'.",
        )

        is DotnetWorkspaceResolutionIssue.ProjectNotConfigured -> ResolutionDiagnostic(
            code = "dotnet.workspace.project-not-configured",
            message = "Dotnet project not configured for service '${issue.serviceName}'.",
        )

        is DotnetWorkspaceResolutionIssue.UnknownModelReference -> ResolutionDiagnostic(
            code = "dotnet.workspace.unknown-model-reference",
            message = "Dotnet model '${issue.modelName}' in service '${issue.serviceName}' " +
                "references unknown model '${issue.targetName}'.",
        )
    }
}
