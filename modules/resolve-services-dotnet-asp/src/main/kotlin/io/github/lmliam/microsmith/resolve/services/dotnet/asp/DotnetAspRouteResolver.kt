package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.core.support.validateDotnetIdentifier

internal class DotnetAspRouteResolver {
    fun parseDeclaredRoute(route: String, label: String, allowEmpty: Boolean = false): List<String> {
        val normalized = route.trim()
        if (normalized.isEmpty()) {
            require(allowEmpty) { "$label cannot be blank." }
            return emptyList()
        }

        require(normalized.startsWith("/")) {
            "$label must start with '/': '$route'."
        }
        require("//" !in normalized) {
            "$label cannot contain empty path segments: '$route'."
        }

        return normalized
            .split('/')
            .filter(String::isNotBlank)
            .map { segment -> validateRouteSegment(segment, route, label) }
    }

    fun normalizeRoute(segments: List<String>): String {
        return if (segments.isEmpty()) {
            "/"
        } else {
            "/" + segments.joinToString("/")
        }
    }

    fun extractRoutePlaceholders(segments: List<String>, route: String): List<String> {
        val placeholders =
            segments
                .filter { it.startsWith("{") && it.endsWith("}") }
                .map { it.substring(1, it.length - 1) }

        val collisions =
            placeholders
                .groupBy { it }
                .filterValues { it.size > 1 }
                .keys
                .sorted()
        require(collisions.isEmpty()) {
            "ASP.NET route '$route' declares duplicate placeholders: ${collisions.joinToString(", ")}."
        }

        return placeholders
    }

    private fun validateRouteSegment(segment: String, route: String, label: String): String {
        val hasOpeningBrace = '{' in segment
        val hasClosingBrace = '}' in segment
        if (!hasOpeningBrace && !hasClosingBrace) {
            return segment
        }

        require(
            hasOpeningBrace &&
                hasClosingBrace &&
                segment.startsWith("{") &&
                segment.endsWith("}") &&
                segment.count { it == '{' } == 1 &&
                segment.count { it == '}' } == 1,
        ) {
            "$label contains invalid route segment '$segment' in '$route'. " +
                "Placeholders must occupy a whole segment like '/{id}'."
        }

        val placeholder = segment.substring(1, segment.length - 1)
        require(placeholder.isNotBlank() && placeholder == placeholder.trim()) {
            "$label contains blank or padded placeholder '$segment' in '$route'."
        }
        validateDotnetIdentifier(placeholder, "Route placeholder")
        return segment
    }
}
