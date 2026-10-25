package io.github.lmliam.microsmith.resolve.services.dotnet

sealed interface DotnetWorkspaceResolutionIssue : DotnetResolutionIssue {
    data class TargetNotConfigured(val serviceName: String) : DotnetWorkspaceResolutionIssue

    data class SolutionNotConfigured(val serviceName: String) : DotnetWorkspaceResolutionIssue

    data class SolutionNotDeclared(val serviceName: String, val solutionName: String) : DotnetWorkspaceResolutionIssue

    data class ProjectNotConfigured(val serviceName: String) : DotnetWorkspaceResolutionIssue

    data class UnknownModelReference(val serviceName: String, val modelName: String, val targetName: String) :
        DotnetWorkspaceResolutionIssue
}
