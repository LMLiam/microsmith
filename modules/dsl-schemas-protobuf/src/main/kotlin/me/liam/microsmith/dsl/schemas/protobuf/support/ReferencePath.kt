package me.liam.microsmith.dsl.schemas.protobuf.support

internal fun getReferencePath(currentSegments: List<String>, target: String): List<String> {
    require(target.isNotBlank()) { "Reference target cannot be blank." }

    return when {
        !target.startsWith(".") ->
            if ('.' in target) {
                target.validateReferencePathSegments("Reference target")
            } else {
                currentSegments + target
            }

        else -> {
            val upCount = target.takeWhile { it == '.' }.length
            val remaining = target.drop(upCount)
            require(remaining.isNotBlank()) { "Reference target cannot end with only dots: '$target'" }

            currentSegments.dropLast(upCount.coerceAtMost(currentSegments.size)) +
                remaining.validateReferencePathSegments("Reference target")
        }
    }
}

private fun String.validateReferencePathSegments(label: String): List<String> {
    val segments = split('.')
    require(segments.none(String::isBlank)) { "$label contains empty path segments: '$this'" }
    return segments
}
