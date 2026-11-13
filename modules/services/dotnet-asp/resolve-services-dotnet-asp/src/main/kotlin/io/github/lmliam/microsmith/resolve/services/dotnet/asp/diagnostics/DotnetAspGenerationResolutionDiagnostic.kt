package io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics

import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.naming.MICROSMITH_CONTROLLER_BASE_TYPE_NAME
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.naming.RESULT_BODY_PROPERTY_NAME

internal fun DotnetAspGenerationResolutionIssue.toDiagnostic(): ResolutionDiagnostic =
    when (this) {
        is DotnetAspGenerationResolutionIssue.ResponseHeaderCollidesWithBody ->
            ResolutionDiagnostic(
                code = "dotnet.asp.response-header-body-collision",
                message =
                    "ASP.NET response $statusCode in operation '$operationName' for service '$serviceName' " +
                        "declares header '$headerName', which collides with the generated result body property " +
                        "'$RESULT_BODY_PROPERTY_NAME'.",
            )

        is DotnetAspGenerationResolutionIssue.ResponseHeaderPropertyCollision ->
            ResolutionDiagnostic(
                code = "dotnet.asp.response-header-collision",
                message =
                    "ASP.NET response $statusCode in operation '$operationName' for service '$serviceName' " +
                        "declares headers with colliding generated property names: " +
                        "${propertyNames.joinToString(", ")}.",
            )

        is DotnetAspGenerationResolutionIssue.ResponseHeaderWithoutPropertyName ->
            ResolutionDiagnostic(
                code = "dotnet.asp.response-header-invalid",
                message =
                    "ASP.NET response $statusCode in operation '$operationName' for service '$serviceName' " +
                        "declares header '$headerName', which has no letters or digits to name a generated property.",
            )

        is DotnetAspGenerationResolutionIssue.NoContentResponseWithBody ->
            ResolutionDiagnostic(
                code = "dotnet.asp.no-content-response-body",
                message =
                    "ASP.NET response 204 in operation '$operationName' for service '$serviceName' cannot declare " +
                        "response body fields for model '$modelName'. HTTP 204 responses are emitted without a " +
                        "response body.",
            )

        is DotnetAspGenerationResolutionIssue.ControllerBaseTypeCollision ->
            ResolutionDiagnostic(
                code = "dotnet.asp.controller-base-type-collision",
                message =
                    "ASP.NET service '$serviceName' project '$projectName' generates controller base type " +
                        "'$typeName', which collides with shared generated controller base type " +
                        "'$MICROSMITH_CONTROLLER_BASE_TYPE_NAME'.",
            )

        is DotnetAspGenerationResolutionIssue.ContractTypeCollision ->
            ResolutionDiagnostic(
                code = "dotnet.asp.contract-type-collision",
                message =
                    "ASP.NET service '$serviceName' produces generated contract type '$typeName' more than once: " +
                        "${owners.joinToString(", ")}.",
            )
    }
