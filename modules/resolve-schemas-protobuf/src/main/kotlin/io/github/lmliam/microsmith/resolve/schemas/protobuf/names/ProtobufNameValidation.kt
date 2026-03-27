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
        val normalized = value.trim()
        require(normalized.isNotBlank()) { "$label cannot be blank." }
        require(normalized == value && PROTO_IDENTIFIER.matches(normalized)) {
            "$label is not a valid protobuf identifier: '$value'"
        }
    }

    private val PROTO_IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")
}
