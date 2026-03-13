package io.github.lmliam.microsmith.cli.plugins

private const val REDACTED_SECRET = "<redacted>"

internal fun String.redactSensitiveValues(sensitiveValues: Set<String>): String = sensitiveValues
    .asSequence()
    .map(String::trim)
    .filter(String::isNotEmpty)
    .distinct()
    .sortedWith(compareByDescending<String> { secret -> secret.length }.thenBy { secret -> secret })
    .fold(this) { sanitized, secret -> sanitized.replace(secret, REDACTED_SECRET) }
