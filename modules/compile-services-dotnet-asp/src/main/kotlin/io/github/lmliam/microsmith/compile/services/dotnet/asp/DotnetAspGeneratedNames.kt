package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspEndpointArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspResponseArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import java.util.Locale

internal const val MICROSMITH_CONTROLLER_BASE_TYPE_NAME = "MicrosmithControllerBase"
internal const val RESULT_BODY_PROPERTY_NAME = "Body"

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

internal fun resultVariantTypeName(
    endpoint: DotnetAspEndpointArtifact,
    response: DotnetAspResponseArtifact,
): String = endpoint.operationName + dotnetAspStatusName(response.statusCode)

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

internal fun dotnetAspStatusName(statusCode: Int): String = when (statusCode) {
    200 -> "Ok"
    201 -> "Created"
    202 -> "Accepted"
    204 -> "NoContent"
    400 -> "BadRequest"
    401 -> "Unauthorized"
    403 -> "Forbidden"
    404 -> "NotFound"
    409 -> "Conflict"
    500 -> "InternalServerError"
    else -> "Status$statusCode"
}

private val HEADER_PROPERTY_DELIMITER_PATTERN = Regex("[^A-Za-z0-9]+")
