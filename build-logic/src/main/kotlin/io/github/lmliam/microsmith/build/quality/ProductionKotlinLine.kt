package io.github.lmliam.microsmith.build.quality

internal fun String.packageNameOrNull(): String? {
    val line = trim().removeTrailingInlineComment()
    return PACKAGE_DECLARATION_PATTERN.matchEntire(line)?.groupValues?.get(1)
}

internal fun String.isTopLevelProductionDeclarationLine(): Boolean {
    val declarationLine = trimStart().removeLeadingInlineAnnotations()
    if (declarationLine.isBlank() || declarationLine.startsWith("private ")) {
        return false
    }

    return TOP_LEVEL_TYPE_DECLARATION_PATTERN.containsMatchIn(declarationLine) ||
        TOP_LEVEL_FUN_INTERFACE_PATTERN.containsMatchIn(declarationLine) ||
        TOP_LEVEL_TYPE_ALIAS_PATTERN.containsMatchIn(declarationLine)
}

internal fun String.topLevelProductionDeclarationNameOrNull(): String? {
    val declarationLine = trimStart().removeLeadingInlineAnnotations()
    if (declarationLine.isBlank() || declarationLine.startsWith("private ")) {
        return null
    }

    return TOP_LEVEL_TYPE_DECLARATION_NAME_PATTERN.find(declarationLine)?.groupValues?.get(1)
        ?: TOP_LEVEL_FUN_INTERFACE_NAME_PATTERN.find(declarationLine)?.groupValues?.get(1)
        ?: TOP_LEVEL_TYPE_ALIAS_NAME_PATTERN.find(declarationLine)?.groupValues?.get(1)
}

private fun String.removeTrailingInlineComment(): String = substringBefore("//")
    .substringBefore("/*")
    .trimEnd()

private fun String.removeLeadingInlineAnnotations(): String {
    var index = indexOfFirst { character -> !character.isWhitespace() }
    if (index == -1) {
        return ""
    }

    while (index < length && this[index] == '@') {
        index = consumeInlineAnnotation(index) ?: return trimStart()
        while (index < length && this[index].isWhitespace()) {
            index += 1
        }
    }

    return substring(index)
}

private fun String.consumeInlineAnnotation(startIndex: Int): Int? {
    var index = startIndex + 1
    index = consumeQualifiedIdentifier(index) ?: return null
    if (index < length && this[index] == ':') {
        index = consumeQualifiedIdentifier(index + 1) ?: return null
    }
    if (index < length && this[index] == '(') {
        index = consumeBalancedParentheses(index) ?: return null
    }
    return index
}

private fun String.consumeQualifiedIdentifier(startIndex: Int): Int? {
    var index = startIndex
    var consumedSegment = false
    while (index < length) {
        val segmentEnd = consumeIdentifierSegment(index) ?: break
        consumedSegment = true
        index = segmentEnd
        if (index >= length || this[index] != '.') {
            break
        }
        index += 1
    }
    return index.takeIf { consumedSegment }
}

private fun String.consumeIdentifierSegment(startIndex: Int): Int? {
    if (startIndex >= length || !this[startIndex].isKotlinIdentifierStart()) {
        return null
    }

    var index = startIndex + 1
    while (index < length && this[index].isKotlinIdentifierPart()) {
        index += 1
    }
    return index
}

private fun Char.isKotlinIdentifierStart(): Boolean = this == '_' || isLetter()

private fun Char.isKotlinIdentifierPart(): Boolean = isKotlinIdentifierStart() || isDigit()

private fun String.consumeBalancedParentheses(startIndex: Int): Int? {
    var index = startIndex
    var depth = 0
    while (index < length) {
        when {
            startsWith(KOTLIN_TRIPLE_QUOTE, index) -> {
                index = consumeTripleQuotedString(index) ?: return null
                continue
            }

            this[index] == '"' -> {
                index = consumeQuotedLiteral(index, '"') ?: return null
                continue
            }

            this[index] == '\'' -> {
                index = consumeQuotedLiteral(index, '\'') ?: return null
                continue
            }

            this[index] == '(' -> depth += 1
            this[index] == ')' -> {
                depth -= 1
                if (depth == 0) {
                    return index + 1
                }
            }
        }
        index += 1
    }
    return null
}

private fun String.consumeTripleQuotedString(startIndex: Int): Int? {
    val closingIndex = indexOf(KOTLIN_TRIPLE_QUOTE, startIndex + KOTLIN_TRIPLE_QUOTE.length)
    return closingIndex.takeIf { it >= 0 }?.plus(KOTLIN_TRIPLE_QUOTE.length)
}

private val PACKAGE_DECLARATION_PATTERN = Regex("^package\\s+([A-Za-z0-9_.]+)$")
private const val DECLARATION_MODIFIER_PATTERN =
    "(?:(?:public|internal|open|abstract|final|sealed|data|value|enum|annotation|expect|actual)\\s+)*"
private val TOP_LEVEL_TYPE_DECLARATION_PATTERN = Regex(
    "^$DECLARATION_MODIFIER_PATTERN(?:class|interface|object)\\b",
)
private val TOP_LEVEL_FUN_INTERFACE_PATTERN = Regex("^${DECLARATION_MODIFIER_PATTERN}fun\\s+interface\\b")
private val TOP_LEVEL_TYPE_ALIAS_PATTERN = Regex("^${DECLARATION_MODIFIER_PATTERN}typealias\\b")
private val TOP_LEVEL_TYPE_DECLARATION_NAME_PATTERN = Regex(
    "^$DECLARATION_MODIFIER_PATTERN(?:class|interface|object)\\s+([A-Za-z_][A-Za-z0-9_]*)\\b",
)
private val TOP_LEVEL_FUN_INTERFACE_NAME_PATTERN = Regex(
    "^${DECLARATION_MODIFIER_PATTERN}fun\\s+interface\\s+([A-Za-z_][A-Za-z0-9_]*)\\b",
)
private val TOP_LEVEL_TYPE_ALIAS_NAME_PATTERN = Regex(
    "^${DECLARATION_MODIFIER_PATTERN}typealias\\s+([A-Za-z_][A-Za-z0-9_]*)\\b",
)
