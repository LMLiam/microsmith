package io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.endpoint.DotnetAspHttpMethod
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import java.nio.file.Path

class DotnetAspResolutionDiagnosticMapperTests :
    StringSpec(
        {
            "maps asp net issues to diagnostics" {
                val mapper = DotnetAspResolutionDiagnosticMapper()

                listOf(
                        DotnetAspResolutionIssue.DuplicateOperationName(
                            serviceName = "UserService",
                            operationName = "GetUser",
                        ),
                        DotnetAspResolutionIssue.DuplicateRestEndpoint(
                            serviceName = "UserService",
                            method = DotnetAspHttpMethod.GET,
                            route = "/users",
                        ),
                        DotnetAspResolutionIssue.OutputRootCollision(
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
                        DotnetAspResolutionIssue.PortOutOfRange(
                            serviceName = "UserService",
                            kind = DotnetAspResolutionIssue.PortKind.HTTPS,
                            port = 65_536,
                        ),
                        DotnetAspResolutionIssue.LaunchPortCollision(
                            port = 7000,
                            serviceNames = listOf("AdminService", "UserService"),
                        ),
                        DotnetAspResolutionIssue.NoFreeDefaultPorts(serviceName = "UserService"),
                    )
                    .map(mapper::map) shouldContainExactly
                    listOf(
                        ResolutionDiagnostic(
                            code = "dotnet.asp.duplicate-operation-name",
                            message =
                                "ASP.NET service 'UserService' " + "declares duplicate operation " + "name 'GetUser'.",
                        ),
                        ResolutionDiagnostic(
                            code = "dotnet.asp.duplicate-rest-endpoint",
                            message =
                                "ASP.NET service 'UserService' " + "declares duplicate REST endpoint: " + "GET /users.",
                        ),
                        ResolutionDiagnostic(
                            code = "dotnet.asp.output-root-collision",
                            message =
                                "ASP.NET services AdminService, " +
                                    "UserService resolve to colliding " +
                                    "output root " +
                                    "'dotnet/Platform/Shared.Api'.",
                        ),
                        ResolutionDiagnostic(
                            code = "dotnet.asp.port-out-of-range",
                            message =
                                "ASP.NET service 'UserService' resolves its HTTPS port to 65536, " +
                                    "which must be between 1 and 65535.",
                        ),
                        ResolutionDiagnostic(
                            code = "dotnet.asp.launch-port-collision",
                            message = "ASP.NET services AdminService, UserService share launch port 7000.",
                        ),
                        ResolutionDiagnostic(
                            code = "dotnet.asp.no-free-default-ports",
                            message = "ASP.NET service 'UserService' has no free default launch ports left.",
                        ),
                    )
            }
        },
    )
