package io.github.lmliam.microsmith.runtime.scripting.symbols.parsing

import io.github.lmliam.microsmith.runtime.scripting.api.ScriptCallCallee
import io.github.lmliam.microsmith.runtime.scripting.api.ScriptLiteral
import org.jetbrains.kotlin.lexer.KtTokens

internal object KotlinScriptLiteralParser {
    fun parseCallee(tokens: List<KotlinScriptToken>, index: Int): ParsedCallee? {
        val token = tokens[index]

        return when {
            token.type == KtTokens.OPEN_QUOTE -> {
                val parsedString = parseStringLiteral(tokens, index) ?: return null

                ParsedCallee(
                    callee = parsedString.value?.let(ScriptCallCallee::StringLiteral) ?: ScriptCallCallee.Other,
                    nextIndex = parsedString.nextIndex,
                )
            }

            token.type == KtTokens.INTEGER_LITERAL ->
                ParsedCallee(
                    callee =
                        token.text.toKotlinIntOrNull()?.let(ScriptCallCallee::IntLiteral) ?: ScriptCallCallee.Other,
                    nextIndex = index + 1,
                )

            token.text.isIdentifierLike() ->
                ParsedCallee(
                    callee = ScriptCallCallee.Named(token.text.removeSurrounding("`")),
                    nextIndex = index + 1,
                )

            else -> null
        }
    }

    fun parseLiteral(tokens: List<KotlinScriptToken>, start: Int, endExclusive: Int): ScriptLiteral? {
        if (start >= endExclusive) return null

        val first = tokens[start]

        if (first.type == KtTokens.OPEN_QUOTE) {
            val parsedString = parseStringLiteral(tokens, start) ?: return null
            if (parsedString.nextIndex != endExclusive) return null
            return parsedString.value?.let(ScriptLiteral::StringValue)
        }

        if (endExclusive != start + 1) return null

        return when (first.type) {
            KtTokens.INTEGER_LITERAL -> first.text.toKotlinIntOrNull()?.let(ScriptLiteral::IntValue)

            KtTokens.TRUE_KEYWORD -> ScriptLiteral.BooleanValue(true)

            KtTokens.FALSE_KEYWORD -> ScriptLiteral.BooleanValue(false)

            else -> null
        }
    }

    private fun parseStringLiteral(tokens: List<KotlinScriptToken>, openingQuoteIndex: Int): ParsedString? {
        val value = StringBuilder()
        var dynamic = false
        var index = openingQuoteIndex + 1

        while (index < tokens.size) {
            val token = tokens[index]

            when (token.type) {
                KtTokens.CLOSING_QUOTE ->
                    return ParsedString(
                        value = value.toString().takeUnless { dynamic },
                        nextIndex = index + 1,
                    )

                KtTokens.REGULAR_STRING_PART,
                KtTokens.DANGLING_NEWLINE -> value.append(token.text)

                KtTokens.ESCAPE_SEQUENCE -> value.append(token.text.decodeKotlinEscape())

                KtTokens.SHORT_TEMPLATE_ENTRY_START,
                KtTokens.LONG_TEMPLATE_ENTRY_START -> dynamic = true
            }

            index++
        }

        return null
    }

    private fun String.isIdentifierLike(): Boolean = removeSurrounding("`").matches(IDENTIFIER_PATTERN)

    private fun String.toKotlinIntOrNull(): Int? {
        val normalized = replace("_", "")

        return when {
            normalized.startsWith(HEX_PREFIX, ignoreCase = true) ->
                normalized.drop(RADIX_PREFIX_LENGTH).toIntOrNull(HEX_RADIX)

            normalized.startsWith(BINARY_PREFIX, ignoreCase = true) ->
                normalized.drop(RADIX_PREFIX_LENGTH).toIntOrNull(BINARY_RADIX)

            else -> normalized.toIntOrNull()
        }
    }

    private fun String.decodeKotlinEscape(): String =
        when (this) {
            "\\t" -> "\t"

            "\\b" -> "\b"

            "\\n" -> "\n"

            "\\r" -> "\r"

            "\\'" -> "'"

            "\\\"" -> "\""

            "\\\\" -> "\\"

            "\\$" -> "$"

            else ->
                if (startsWith(UNICODE_ESCAPE_PREFIX) && length == UNICODE_ESCAPE_LENGTH) {
                    substring(UNICODE_ESCAPE_PREFIX.length).toIntOrNull(HEX_RADIX)?.toChar()?.toString()
                        ?: removePrefix("\\")
                } else {
                    removePrefix("\\")
                }
        }

    data class ParsedCallee(val callee: ScriptCallCallee, val nextIndex: Int)

    private data class ParsedString(val value: String?, val nextIndex: Int)

    private const val HEX_PREFIX = "0x"
    private const val BINARY_PREFIX = "0b"
    private const val HEX_RADIX = 16
    private const val BINARY_RADIX = 2
    private const val RADIX_PREFIX_LENGTH = 2

    private const val UNICODE_ESCAPE_PREFIX = "\\u"
    private const val UNICODE_ESCAPE_LENGTH = 6

    private val IDENTIFIER_PATTERN = Regex("[A-Za-z_][A-Za-z0-9_]*")
}
