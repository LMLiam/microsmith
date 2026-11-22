package io.github.lmliam.microsmith.runtime.scripting.symbols.parsing

import io.github.lmliam.microsmith.runtime.scripting.api.ScriptCallCallee
import io.github.lmliam.microsmith.runtime.scripting.api.ScriptCallSite
import io.github.lmliam.microsmith.runtime.scripting.api.ScriptSymbolDeclarationSite
import org.jetbrains.kotlin.lexer.KtTokens

internal class KotlinScriptCallSiteParser(
    private val tokenizer: KotlinScriptTokenizer,
    private val callParser: KotlinScriptCallParser,
) {
    fun declarations(
        @Suppress("UNUSED_PARAMETER") sourceName: String,
        sourceText: String,
        declarationCallNames: Set<String>,
    ): List<ScriptSymbolDeclarationSite> {
        val tokens = tokenizer.tokenize(sourceText)
        val callsByOpeningBrace = mutableMapOf<Int, ScriptCallSite>()
        val enclosingBlocks = mutableListOf<ScriptCallSite?>()
        val declarations = mutableListOf<ScriptSymbolDeclarationSite>()

        for (index in tokens.indices) {
            when (tokens[index].type) {
                KtTokens.LBRACE -> enclosingBlocks += callsByOpeningBrace[index]

                KtTokens.RBRACE ->
                    if (enclosingBlocks.isNotEmpty()) {
                        enclosingBlocks.removeLast()
                    }

                else -> {
                    val parsedCall = callParser.parse(tokens, index) ?: continue

                    parsedCall.openingBraceIndex?.let { braceIndex ->
                        callsByOpeningBrace[braceIndex] = parsedCall.call
                    }

                    val callName = (parsedCall.call.callee as? ScriptCallCallee.Named)?.name

                    if (callName in declarationCallNames) {
                        declarations +=
                            ScriptSymbolDeclarationSite(
                                call = parsedCall.call,
                                enclosingCalls = enclosingBlocks.filterNotNull(),
                            )
                    }
                }
            }
        }

        return declarations
    }
}
