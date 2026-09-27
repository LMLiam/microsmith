package io.github.lmliam.microsmith.runtime.scripting.symbols.parsing
import io.github.lmliam.microsmith.runtime.scripting.api.ScriptCallCallee
import io.github.lmliam.microsmith.runtime.scripting.api.ScriptCallSite
import io.github.lmliam.microsmith.runtime.scripting.api.ScriptLiteral
import org.jetbrains.kotlin.lexer.KtTokens

internal object KotlinScriptCallParser {
    fun parse(tokens: List<KotlinScriptToken>, index: Int): ParsedCall? {
        val callee =
            KotlinScriptLiteralParser.parseCallee(tokens, index)
                ?: return null

        if (
            callee.callee is ScriptCallCallee.Named &&
            index > 0 &&
            tokens[index - 1].isQualifiedAccess()
        ) {
            return null
        }

        val arguments =
            if (tokens.getOrNull(callee.nextIndex)?.type == KtTokens.LPAR) {
                parseArguments(tokens, callee.nextIndex)
                    ?: return null
            } else {
                null
            }

        val afterCall = arguments?.nextIndex ?: callee.nextIndex

        val openingBraceIndex =
            afterCall.takeIf { candidateIndex ->
                tokens.getOrNull(candidateIndex)?.type == KtTokens.LBRACE
            }

        if (arguments == null && openingBraceIndex == null) return null

        return ParsedCall(
            call =
            ScriptCallSite(
                callee = callee.callee,
                arguments = arguments?.values.orEmpty(),
            ),
            openingBraceIndex = openingBraceIndex,
        )
    }

    private fun parseArguments(tokens: List<KotlinScriptToken>, openingParenthesisIndex: Int): ParsedArguments? {
        val arguments = mutableListOf<ScriptLiteral?>()
        var parenthesisDepth = 1
        var bracketDepth = 0
        var braceDepth = 0
        var argumentStart = openingParenthesisIndex + 1
        var index = argumentStart

        while (index < tokens.size) {
            when (tokens[index].type) {
                KtTokens.LPAR ->
                    parenthesisDepth++

                KtTokens.RPAR -> {
                    parenthesisDepth--

                    if (parenthesisDepth == 0) {
                        if (argumentStart < index) {
                            arguments +=
                                KotlinScriptLiteralParser.parseLiteral(
                                    tokens,
                                    argumentStart,
                                    index,
                                )
                        }

                        return ParsedArguments(
                            values = arguments,
                            nextIndex = index + 1,
                        )
                    }
                }

                KtTokens.LBRACKET ->
                    bracketDepth++

                KtTokens.RBRACKET ->
                    bracketDepth--

                KtTokens.LBRACE ->
                    braceDepth++

                KtTokens.RBRACE ->
                    braceDepth--

                KtTokens.COMMA ->
                    if (
                        parenthesisDepth == 1 &&
                        bracketDepth == 0 &&
                        braceDepth == 0
                    ) {
                        arguments +=
                            KotlinScriptLiteralParser.parseLiteral(
                                tokens,
                                argumentStart,
                                index,
                            )

                        argumentStart = index + 1
                    }
            }

            index++
        }

        return null
    }

    private fun KotlinScriptToken.isQualifiedAccess(): Boolean = type == KtTokens.DOT ||
        type == KtTokens.SAFE_ACCESS ||
        type == KtTokens.COLONCOLON

    data class ParsedCall(val call: ScriptCallSite, val openingBraceIndex: Int?)

    private data class ParsedArguments(val values: List<ScriptLiteral?>, val nextIndex: Int)
}
