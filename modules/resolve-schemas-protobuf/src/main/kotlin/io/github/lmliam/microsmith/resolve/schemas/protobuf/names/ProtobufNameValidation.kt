package io.github.lmliam.microsmith.resolve.schemas.protobuf.names

object ProtobufNameValidation {
    fun normalizeQualifiedName(value: String, label: String): String {
        val normalized = value.trim()
        require(normalized.isNotBlank()) { "$label cannot be blank." }
        require(!normalized.startsWith(".")) { "$label cannot start with '.': '$value'" }
        require(!normalized.endsWith(".")) { "$label cannot end with '.': '$value'" }
        require(normalized.none(Char::isWhitespace)) { "$label cannot contain whitespace: '$value'" }

        val segments = normalized.split('.')
        require(segments.none(String::isBlank)) { "$label contains an empty segment: '$value'" }
        segments.forEachIndexed { index, segment ->
            requireIdentifier(segment, "$label segment[$index]")
        }

        return segments.joinToString(".")
    }

    fun requireIdentifier(value: String, label: String) {
        require(isIdentifier(value)) {
            "$label is not a valid protobuf identifier: '$value'"
        }
    }

    fun isIdentifier(value: String): Boolean = value.isNotBlank() &&
        value == value.trim() &&
        PROTO_IDENTIFIER.matches(value)

    private val PROTO_IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")
}
