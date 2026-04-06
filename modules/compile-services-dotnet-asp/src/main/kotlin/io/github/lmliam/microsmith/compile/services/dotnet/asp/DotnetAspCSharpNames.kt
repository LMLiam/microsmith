package io.github.lmliam.microsmith.compile.services.dotnet.asp

import java.util.Locale

private const val HTTP_STATUS_OK = 200
private const val HTTP_STATUS_CREATED = 201
private const val HTTP_STATUS_ACCEPTED = 202
private const val HTTP_STATUS_NO_CONTENT = 204
private const val HTTP_STATUS_BAD_REQUEST = 400
private const val HTTP_STATUS_UNAUTHORIZED = 401
private const val HTTP_STATUS_FORBIDDEN = 403
private const val HTTP_STATUS_NOT_FOUND = 404
private const val HTTP_STATUS_CONFLICT = 409
private const val HTTP_STATUS_INTERNAL_SERVER_ERROR = 500

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
    HTTP_STATUS_OK -> "Ok"
    HTTP_STATUS_CREATED -> "Created"
    HTTP_STATUS_ACCEPTED -> "Accepted"
    HTTP_STATUS_NO_CONTENT -> "NoContent"
    HTTP_STATUS_BAD_REQUEST -> "BadRequest"
    HTTP_STATUS_UNAUTHORIZED -> "Unauthorized"
    HTTP_STATUS_FORBIDDEN -> "Forbidden"
    HTTP_STATUS_NOT_FOUND -> "NotFound"
    HTTP_STATUS_CONFLICT -> "Conflict"
    HTTP_STATUS_INTERNAL_SERVER_ERROR -> "InternalServerError"
    else -> "Status$statusCode"
}

private val HEADER_PROPERTY_DELIMITER_PATTERN = Regex("[^A-Za-z0-9]+")
