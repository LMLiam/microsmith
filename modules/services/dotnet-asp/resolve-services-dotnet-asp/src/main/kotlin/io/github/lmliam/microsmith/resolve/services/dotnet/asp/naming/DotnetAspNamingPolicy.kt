package io.github.lmliam.microsmith.resolve.services.dotnet.asp.naming

import io.github.lmliam.microsmith.InternalMicrosmithApi
import java.util.Locale

@InternalMicrosmithApi
public class DotnetAspNamingPolicy {
    public fun controllerBaseTypeName(projectName: String): String = "${typeName(projectName)}ControllerBase"

    public fun resultBaseTypeName(operationName: String): String = "${operationName}Result"

    public fun resultVariantTypeName(operationName: String, statusCode: Int): String =
        operationName + statusName(statusCode)

    internal fun headerPropertyName(headerName: String): String? =
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

    public fun requiredHeaderPropertyName(headerName: String): String =
        checkNotNull(headerPropertyName(headerName)) {
            "Response header '$headerName' has no generated property name; resolution should have reported it."
        }

    public fun pascalIdentifier(identifier: String): String =
        when {
            identifier.startsWith("@") && identifier.length > 1 ->
                "@${identifier.substring(1).replaceFirstChar { firstChar ->
            firstChar.titlecase(Locale.ROOT)
        }}"

            else -> identifier.replaceFirstChar { firstChar -> firstChar.titlecase(Locale.ROOT) }
        }

    private fun statusName(statusCode: Int): String = COMMON_STATUS_NAMES[statusCode] ?: "Status$statusCode"

    private fun typeName(raw: String): String =
        raw.split('.', '-', '_', ' ')
            .filter(String::isNotBlank)
            .joinToString("") { segment ->
                segment.removePrefix("@").replaceFirstChar { firstChar -> firstChar.titlecase(Locale.ROOT) }
            }
            .ifBlank { error("Unable to derive a generated ASP.NET type name from '$raw'.") }
}

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
