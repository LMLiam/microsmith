package io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics

import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly

class DotnetAspGenerationResolutionDiagnosticTests :
    StringSpec({
        "maps generation conflicts to diagnostics" {
            val mapper = DotnetAspResolutionDiagnosticMapper()

            listOf(
                    DotnetAspGenerationResolutionIssue.ResponseHeaderCollidesWithBody(
                        serviceName = "UserService",
                        operationName = "GetUser",
                        statusCode = 200,
                        headerName = "body",
                    ),
                    DotnetAspGenerationResolutionIssue.ResponseHeaderPropertyCollision(
                        serviceName = "UserService",
                        operationName = "GetUser",
                        statusCode = 200,
                        propertyNames = listOf("XRequestId"),
                    ),
                    DotnetAspGenerationResolutionIssue.ResponseHeaderWithoutPropertyName(
                        serviceName = "UserService",
                        operationName = "GetUser",
                        statusCode = 200,
                        headerName = "---",
                    ),
                    DotnetAspGenerationResolutionIssue.NoContentResponseWithBody(
                        serviceName = "UserService",
                        operationName = "DeleteUser",
                        modelName = "User",
                    ),
                    DotnetAspGenerationResolutionIssue.ControllerBaseTypeCollision(
                        serviceName = "UserService",
                        projectName = "Microsmith",
                        typeName = "MicrosmithControllerBase",
                    ),
                    DotnetAspGenerationResolutionIssue.ContractTypeCollision(
                        serviceName = "UserService",
                        typeName = "GetUserResult",
                        owners = listOf("result base for operation 'GetUser'", "shared model 'GetUserResult'"),
                    ),
                )
                .map(mapper::map) shouldContainExactly
                listOf(
                    ResolutionDiagnostic(
                        code = "dotnet.asp.response-header-body-collision",
                        message =
                            "ASP.NET response 200 in operation 'GetUser' for service 'UserService' declares " +
                                "header 'body', which collides with the generated result body property 'Body'.",
                    ),
                    ResolutionDiagnostic(
                        code = "dotnet.asp.response-header-collision",
                        message =
                            "ASP.NET response 200 in operation 'GetUser' for service 'UserService' declares " +
                                "headers with colliding generated property names: XRequestId.",
                    ),
                    ResolutionDiagnostic(
                        code = "dotnet.asp.response-header-invalid",
                        message =
                            "ASP.NET response 200 in operation 'GetUser' for service 'UserService' declares " +
                                "header '---', which has no letters or digits to name a generated property.",
                    ),
                    ResolutionDiagnostic(
                        code = "dotnet.asp.no-content-response-body",
                        message =
                            "ASP.NET response 204 in operation 'DeleteUser' for service 'UserService' cannot " +
                                "declare response body fields for model 'User'. HTTP 204 responses are emitted " +
                                "without a response body.",
                    ),
                    ResolutionDiagnostic(
                        code = "dotnet.asp.controller-base-type-collision",
                        message =
                            "ASP.NET service 'UserService' project 'Microsmith' generates controller base type " +
                                "'MicrosmithControllerBase', which collides with shared generated controller " +
                                "base type 'MicrosmithControllerBase'.",
                    ),
                    ResolutionDiagnostic(
                        code = "dotnet.asp.contract-type-collision",
                        message =
                            "ASP.NET service 'UserService' produces generated contract type 'GetUserResult' " +
                                "more than once: result base for operation 'GetUser', shared model 'GetUserResult'.",
                    ),
                )
        }
    })
