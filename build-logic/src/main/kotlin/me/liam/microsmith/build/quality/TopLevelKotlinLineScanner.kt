package me.liam.microsmith.build.quality

internal object TopLevelKotlinLineScanner {
    fun scan(lines: List<String>): List<String> {
        var state = ScanState()
        return lines.filter { line ->
            val isTopLevelLine = state.isTopLevelLine
            state = state.consume(line)
            isTopLevelLine
        }
    }

    private data class ScanState(
        val braceDepth: Int = 0,
        val insideBlockComment: Boolean = false,
        val insideTripleQuotedString: Boolean = false,
    ) {
        val isTopLevelLine: Boolean = braceDepth == 0 && !insideBlockComment && !insideTripleQuotedString

        fun consume(line: String): ScanState {
            var index = 0
            var currentBraceDepth = braceDepth
            var currentBlockComment = insideBlockComment
            var currentTripleQuotedString = insideTripleQuotedString

            while (index < line.length) {
                when {
                    currentBlockComment -> {
                        val blockCommentEnd = line.indexOf(blockCommentEndMarker, index)
                        if (blockCommentEnd == -1) {
                            return copy(insideBlockComment = true)
                        }
                        currentBlockComment = false
                        index = blockCommentEnd + blockCommentEndMarker.length
                    }

                    currentTripleQuotedString -> {
                        val tripleQuoteEnd = line.indexOf(tripleQuote, index)
                        if (tripleQuoteEnd == -1) {
                            return copy(
                                braceDepth = currentBraceDepth,
                                insideBlockComment = false,
                                insideTripleQuotedString = true,
                            )
                        }
                        currentTripleQuotedString = false
                        index = tripleQuoteEnd + tripleQuote.length
                    }

                    line.startsWith(lineCommentMarker, index) -> break
                    line.startsWith(blockCommentStartMarker, index) -> {
                        currentBlockComment = true
                        index += blockCommentStartMarker.length
                    }

                    line.startsWith(tripleQuote, index) -> {
                        currentTripleQuotedString = true
                        index += tripleQuote.length
                    }

                    line[index] == '"' -> {
                        index = line.consumeQuotedLiteral(index, '"') ?: return copy(braceDepth = currentBraceDepth)
                    }

                    line[index] == '\'' -> {
                        index = line.consumeQuotedLiteral(index, '\'') ?: return copy(braceDepth = currentBraceDepth)
                    }

                    line[index] == '{' -> {
                        currentBraceDepth += 1
                        index += 1
                    }

                    line[index] == '}' -> {
                        currentBraceDepth = (currentBraceDepth - 1).coerceAtLeast(0)
                        index += 1
                    }

                    else -> index += 1
                }
            }

            return copy(
                braceDepth = currentBraceDepth,
                insideBlockComment = currentBlockComment,
                insideTripleQuotedString = currentTripleQuotedString,
            )
        }
    }

    private fun String.consumeQuotedLiteral(startIndex: Int, quote: Char): Int? {
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

    private const val lineCommentMarker = "//"
    private const val blockCommentStartMarker = "/*"
    private const val blockCommentEndMarker = "*/"
    private const val tripleQuote = "\"\"\""
}
