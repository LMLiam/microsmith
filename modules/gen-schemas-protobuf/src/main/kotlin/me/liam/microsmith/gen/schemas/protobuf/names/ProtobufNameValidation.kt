package me.liam.microsmith.gen.schemas.protobuf.names

/**
 * Centralized validation utilities for schema/reference names and local identifiers.
 *
 * Kept as explicit utilities instead of `String` extensions to avoid implying behavior
 * belongs on `String` itself.
 */
internal object ProtobufNameValidation {
    /** Validates and normalizes a dotted, fully-qualified identifier. */
    internal fun normalizeQualifiedName(value: String, label: String): String {
        val normalized = value.trim()
        require(normalized.isNotBlank()) { "$label cannot be blank." }
        require(!normalized.startsWith(".")) { "$label cannot start with '.': '$value'" }
        require(!normalized.endsWith(".")) { "$label cannot end with '.': '$value'" }
        require(normalized.none(Char::isWhitespace)) { "$label cannot contain whitespace: '$value'" }

        val segments = normalized.split('.')
        require(segments.none { it.isBlank() }) { "$label contains an empty segment: '$value'" }
        segments.forEachIndexed { index, segment ->
            requireIdentifier(segment, "$label segment[$index]")
        }

        return segments.joinToString(".")
    }

    /** Validates a protobuf identifier with regex `[A-Za-z_][A-Za-z0-9_]*`. */
    internal fun requireIdentifier(value: String, label: String) {
        val normalized = value.trim()
        require(normalized.isNotBlank()) { "$label cannot be blank." }
        require(normalized == value && PROTO_IDENTIFIER.matches(normalized)) {
            "$label is not a valid protobuf identifier: '$value'"
        }
    }

    private val PROTO_IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")
}
