package io.github.lmliam.microsmith.resolve.services.dotnet.asp.naming

import java.util.Locale

const val MICROSMITH_CONTROLLER_BASE_TYPE_NAME = "MicrosmithControllerBase"
const val RESULT_BODY_PROPERTY_NAME = "Body"
const val HTTP_NO_CONTENT_STATUS_CODE = 204

fun dotnetAspControllerBaseTypeName(projectName: String): String = "${dotnetAspTypeName(projectName)}ControllerBase"

fun dotnetAspResultBaseTypeName(operationName: String): String = "${operationName}Result"

fun dotnetAspResultVariantTypeName(operationName: String, statusCode: Int): String =
    operationName + dotnetAspStatusName(statusCode)

private fun dotnetAspStatusName(statusCode: Int): String = COMMON_STATUS_NAMES[statusCode] ?: "Status$statusCode"

private fun dotnetAspTypeName(raw: String): String =
    raw.split('.', '-', '_', ' ')
        .filter(String::isNotBlank)
        .joinToString("") { segment ->
            segment.removePrefix("@").replaceFirstChar { firstChar -> firstChar.titlecase(Locale.ROOT) }
        }
        .ifBlank { error("Unable to derive a generated ASP.NET type name from '$raw'.") }

fun dotnetAspHeaderPropertyName(headerName: String): String? =
    headerName
        .trim()
        .split(HEADER_PROPERTY_DELIMITER_PATTERN)
        .filter(String::isNotBlank)
        .joinToString("") { segment ->
            segment.lowercase(Locale.ROOT).replaceFirstChar { firstChar -> firstChar.titlecase(Locale.ROOT) }
        }
        .let { identifier ->
            if (identifier.firstOrNull()?.isDigit() == true) {
                "Header$identifier"
            } else {
                identifier
            }
        }
        .ifBlank { null }

private val HEADER_PROPERTY_DELIMITER_PATTERN = Regex("[^A-Za-z0-9]+")
private val COMMON_STATUS_NAMES =
    mapOf(
        200 to "Ok",
        201 to "Created",
        202 to "Accepted",
        HTTP_NO_CONTENT_STATUS_CODE to "NoContent",
        400 to "BadRequest",
        401 to "Unauthorized",
        403 to "Forbidden",
        404 to "NotFound",
        409 to "Conflict",
        500 to "InternalServerError",
    )
