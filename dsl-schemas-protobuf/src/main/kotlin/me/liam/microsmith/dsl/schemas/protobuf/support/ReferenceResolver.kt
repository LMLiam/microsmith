package me.liam.microsmith.dsl.schemas.protobuf.support

fun resolveReference(currentSegments: List<String>, target: String): List<String> {
    if (!target.startsWith(".")) {
        return if ('.' in target) {
            target.split(".")
        } else {
            currentSegments + target
        }
    }

    val segments = currentSegments.toMutableList()
    var remaining = target
    while (remaining.startsWith(".")) {
        if (segments.isNotEmpty()) segments.removeLast()
        remaining = remaining.removePrefix(".")
    }
    return segments + remaining.split(".")
}