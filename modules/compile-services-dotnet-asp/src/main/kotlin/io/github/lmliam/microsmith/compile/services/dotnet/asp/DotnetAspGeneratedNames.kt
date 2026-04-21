package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspEndpointArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspResponseArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import java.util.Locale

internal const val MICROSMITH_CONTROLLER_BASE_TYPE_NAME = "MicrosmithControllerBase"
internal const val RESULT_BODY_PROPERTY_NAME = "Body"
internal const val HTTP_OK_STATUS_CODE = 200
internal const val HTTP_CREATED_STATUS_CODE = 201
internal const val HTTP_ACCEPTED_STATUS_CODE = 202
internal const val HTTP_NO_CONTENT_STATUS_CODE = 204
internal const val HTTP_BAD_REQUEST_STATUS_CODE = 400
internal const val HTTP_UNAUTHORIZED_STATUS_CODE = 401
internal const val HTTP_FORBIDDEN_STATUS_CODE = 403
internal const val HTTP_NOT_FOUND_STATUS_CODE = 404
internal const val HTTP_CONFLICT_STATUS_CODE = 409
internal const val HTTP_INTERNAL_SERVER_ERROR_STATUS_CODE = 500

internal fun contractsNamespace(artifact: DotnetAspServiceArtifact): String =
    "${artifact.id.projectName}.Generated.Contracts"

internal fun controllersNamespace(artifact: DotnetAspServiceArtifact): String =
    "${artifact.id.projectName}.Generated.Controllers"

internal fun hostingNamespace(artifact: DotnetAspServiceArtifact): String =
    "${artifact.id.projectName}.Generated.Hosting"

internal fun controllerBaseTypeName(artifact: DotnetAspServiceArtifact): String =
    "${dotnetAspTypeName(artifact.id.projectName)}ControllerBase"

internal fun microsmithControllerBaseRelativePath(): String =
    "Generated/Controllers/$MICROSMITH_CONTROLLER_BASE_TYPE_NAME.cs"

internal fun controllerBaseRelativePath(artifact: DotnetAspServiceArtifact): String =
    "Generated/Controllers/${controllerBaseTypeName(artifact)}.cs"

internal fun resultBaseTypeName(endpoint: DotnetAspEndpointArtifact): String = "${endpoint.operationName}Result"

internal fun resultVariantTypeName(endpoint: DotnetAspEndpointArtifact, response: DotnetAspResponseArtifact): String =
    endpoint.operationName + dotnetAspStatusName(response.statusCode)

internal fun dotnetAspPascalIdentifier(identifier: String): String = when {
    identifier.startsWith("@") && identifier.length > 1 ->
        "@${identifier.substring(1).replaceFirstChar { firstChar ->
            firstChar.titlecase(Locale.ROOT)
        }}"

    else ->
        identifier.replaceFirstChar { firstChar ->
            firstChar.titlecase(Locale.ROOT)
        }
}

internal fun dotnetAspTypeName(raw: String): String = raw
    .split('.', '-', '_', ' ')
    .filter(String::isNotBlank)
    .joinToString("") { segment ->
        segment
            .removePrefix("@")
            .replaceFirstChar { firstChar -> firstChar.titlecase(Locale.ROOT) }
    }.ifBlank {
        error("Unable to derive a generated ASP.NET type name from '$raw'.")
    }

internal fun dotnetAspHeaderPropertyName(headerName: String): String = headerName
    .trim()
    .split(HEADER_PROPERTY_DELIMITER_PATTERN)
    .filter(String::isNotBlank)
    .joinToString("") { segment ->
        segment.lowercase(Locale.ROOT).replaceFirstChar { firstChar ->
            firstChar.titlecase(Locale.ROOT)
        }
    }.let { identifier ->
        if (identifier.firstOrNull()?.isDigit() == true) {
            "Header$identifier"
        } else {
            identifier
        }
    }.ifBlank {
        error("Unable to derive an ASP.NET response header property name from '$headerName'.")
    }

internal fun dotnetAspStatusName(statusCode: Int): String = COMMON_STATUS_NAMES[statusCode] ?: "Status$statusCode"

private val HEADER_PROPERTY_DELIMITER_PATTERN = Regex("[^A-Za-z0-9]+")
private val COMMON_STATUS_NAMES = mapOf(
    HTTP_OK_STATUS_CODE to "Ok",
    HTTP_CREATED_STATUS_CODE to "Created",
    HTTP_ACCEPTED_STATUS_CODE to "Accepted",
    HTTP_NO_CONTENT_STATUS_CODE to "NoContent",
    HTTP_BAD_REQUEST_STATUS_CODE to "BadRequest",
    HTTP_UNAUTHORIZED_STATUS_CODE to "Unauthorized",
    HTTP_FORBIDDEN_STATUS_CODE to "Forbidden",
    HTTP_NOT_FOUND_STATUS_CODE to "NotFound",
    HTTP_CONFLICT_STATUS_CODE to "Conflict",
    HTTP_INTERNAL_SERVER_ERROR_STATUS_CODE to "InternalServerError",
)
