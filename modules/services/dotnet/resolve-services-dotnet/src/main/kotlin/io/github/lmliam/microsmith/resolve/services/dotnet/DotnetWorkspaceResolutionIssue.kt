package io.github.lmliam.microsmith.resolve.services.dotnet

public sealed interface DotnetWorkspaceResolutionIssue : DotnetResolutionIssue {
    public data class TargetNotConfigured(public val serviceName: String) : DotnetWorkspaceResolutionIssue

    public data class SolutionNotConfigured(public val serviceName: String) : DotnetWorkspaceResolutionIssue

    public data class SolutionNotDeclared(public val serviceName: String, public val solutionName: String) :
        DotnetWorkspaceResolutionIssue

    public data class ProjectNotConfigured(public val serviceName: String) : DotnetWorkspaceResolutionIssue

    public data class UnknownModelReference(
        public val serviceName: String,
        public val modelName: String,
        public val targetName: String,
    ) : DotnetWorkspaceResolutionIssue
}
