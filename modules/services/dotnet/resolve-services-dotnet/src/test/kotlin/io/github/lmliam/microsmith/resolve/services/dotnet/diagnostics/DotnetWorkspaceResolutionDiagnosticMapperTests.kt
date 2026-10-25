package io.github.lmliam.microsmith.resolve.services.dotnet.diagnostics

import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.github.lmliam.microsmith.resolve.services.dotnet.DotnetWorkspaceResolutionIssue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly

class DotnetWorkspaceResolutionDiagnosticMapperTests :
    StringSpec({
        "maps dotnet workspace issues to diagnostics" {
            val mapper = DotnetWorkspaceResolutionDiagnosticMapper()

            listOf(
                    DotnetWorkspaceResolutionIssue.TargetNotConfigured("UserService"),
                    DotnetWorkspaceResolutionIssue.SolutionNotConfigured("UserService"),
                    DotnetWorkspaceResolutionIssue.SolutionNotDeclared(
                        serviceName = "UserService",
                        solutionName = "Platform",
                    ),
                    DotnetWorkspaceResolutionIssue.ProjectNotConfigured("UserService"),
                    DotnetWorkspaceResolutionIssue.UnknownModelReference(
                        serviceName = "UserService",
                        modelName = "User",
                        targetName = "MissingUser",
                    ),
                )
                .map(mapper::map) shouldContainExactly
                listOf(
                    ResolutionDiagnostic(
                        code = "dotnet.workspace.target-not-configured",
                        message = "Dotnet target not configured for " + "service 'UserService'.",
                    ),
                    ResolutionDiagnostic(
                        code = "dotnet.workspace.solution-not-configured",
                        message = "Dotnet solution not configured for " + "service 'UserService'.",
                    ),
                    ResolutionDiagnostic(
                        code = "dotnet.workspace.solution-not-declared",
                        message = "Dotnet solution 'Platform' is not " + "declared for service " + "'UserService'.",
                    ),
                    ResolutionDiagnostic(
                        code = "dotnet.workspace.project-not-configured",
                        message = "Dotnet project not configured for " + "service 'UserService'.",
                    ),
                    ResolutionDiagnostic(
                        code = "dotnet.workspace.unknown-model-reference",
                        message =
                            "Dotnet model 'User' in service " +
                                "'UserService' references unknown " +
                                "model 'MissingUser'.",
                    ),
                )
        }
    })
