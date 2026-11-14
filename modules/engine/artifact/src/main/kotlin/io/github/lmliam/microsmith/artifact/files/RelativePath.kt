package io.github.lmliam.microsmith.artifact.files

class RelativePath private constructor(val segments: List<String>) {
    val isRoot: Boolean
        get() = segments.isEmpty()

    fun resolve(vararg parts: String): RelativePath = parse(segments + parts)

    override fun equals(other: Any?): Boolean = other is RelativePath && segments == other.segments

    override fun hashCode(): Int = segments.hashCode()

    override fun toString(): String = if (isRoot) ROOT_TEXT else segments.joinToString(SEPARATOR.toString())

    companion object {
        private const val SEPARATOR = '/'
        private const val WINDOWS_SEPARATOR = '\\'
        private const val ROOT_TEXT = "."
        private const val CURRENT_SEGMENT = "."
        private const val PARENT_SEGMENT = ".."
        private val driveLetter = Regex("^[A-Za-z]:")

        val ROOT = RelativePath(emptyList())

        fun of(vararg parts: String): RelativePath = parse(parts.asList())

        private fun parse(parts: List<String>): RelativePath {
            val segments = parts.flatMap { part ->
                require(!part.startsWith(SEPARATOR) && !part.startsWith(WINDOWS_SEPARATOR)) {
                    "Relative path '$part' must not be absolute."
                }

                part.split(SEPARATOR, WINDOWS_SEPARATOR).filter { it.isNotEmpty() && it != CURRENT_SEGMENT }
            }

            require(segments.none(driveLetter::containsMatchIn)) {
                "Relative path '${parts.joinToString(SEPARATOR.toString())}' must not be absolute."
            }

            require(PARENT_SEGMENT !in segments) {
                "Relative path '${parts.joinToString(SEPARATOR.toString())}' must not contain '$PARENT_SEGMENT'."
            }

            return if (segments.isEmpty()) ROOT else RelativePath(segments)
        }
    }
}
