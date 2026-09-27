package io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.endpoint.DotnetAspHttpMethod
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import java.nio.file.Path

class DotnetAspResolutionDiagnosticMapperTests :
    StringSpec({
        "maps asp net issues to diagnostics" {
            val mapper =
                DotnetAspResolutionDiagnosticMapper()

            listOf(
                DotnetAspResolutionIssue
                    .DuplicateOperationName(
                        serviceName = "UserService",
                        operationName = "GetUser",
                    ),
                DotnetAspResolutionIssue
                    .DuplicateRestEndpoint(
                        serviceName = "UserService",
                        method = DotnetAspHttpMethod.GET,
                        route = "/users",
                    ),
                DotnetAspResolutionIssue
                    .OutputRootCollision(
                        outputRoot =
                        Path.of(
                            "dotnet",
                            "Platform",
                            "Shared.Api",
                        ),
                        serviceNames =
                        listOf(
                            "AdminService",
                            "UserService",
                        ),
                    ),
            ).map(mapper::map) shouldContainExactly
                listOf(
                    ResolutionDiagnostic(
                        code =
                        "dotnet.asp.duplicate-operation-name",
                        message =
                        "ASP.NET service 'UserService' " +
                            "declares duplicate operation " +
                            "name 'GetUser'.",
                    ),
                    ResolutionDiagnostic(
                        code =
                        "dotnet.asp.duplicate-rest-endpoint",
                        message =
                        "ASP.NET service 'UserService' " +
                            "declares duplicate REST endpoint: " +
                            "GET /users.",
                    ),
                    ResolutionDiagnostic(
                        code =
                        "dotnet.asp.output-root-collision",
                        message =
                        "ASP.NET services AdminService, " +
                            "UserService resolve to colliding " +
                            "output root " +
                            "'dotnet/Platform/Shared.Api'.",
                    ),
                )
        }
    })
