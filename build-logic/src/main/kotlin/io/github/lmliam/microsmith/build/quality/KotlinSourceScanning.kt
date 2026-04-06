package io.github.lmliam.microsmith.build.quality

internal const val KOTLIN_TRIPLE_QUOTE = "\"\"\""

internal fun String.consumeQuotedLiteral(startIndex: Int, quote: Char): Int? {
    var index = startIndex + 1
    while (index < length) {
        if (this[index] == '\\') {
            index += 2
            continue
        }
        if (this[index] == quote) {
            return index + 1
        }
        index += 1
    }
    return null
}
