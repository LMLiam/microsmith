package io.github.lmliam.microsmith.runtime.scripting.symbols.parsing
import io.github.lmliam.microsmith.runtime.scripting.api.ScriptCallCallee
import io.github.lmliam.microsmith.runtime.scripting.api.ScriptCallSite
import io.github.lmliam.microsmith.runtime.scripting.api.ScriptSymbolDeclarationSite
import org.jetbrains.kotlin.lexer.KtTokens

internal object KotlinScriptCallSiteParser {
    fun declarations(
        @Suppress("UNUSED_PARAMETER")
        sourceName: String,
        sourceText: String,
        declarationCallNames: Set<String>,
    ): List<ScriptSymbolDeclarationSite> {
        val tokens = KotlinScriptTokenizer.tokenize(sourceText)
        val callsByOpeningBrace = mutableMapOf<Int, ScriptCallSite>()
        val enclosingBlocks = mutableListOf<ScriptCallSite?>()
        val declarations = mutableListOf<ScriptSymbolDeclarationSite>()

        tokens.indices.forEach { index ->
            when (tokens[index].type) {
                KtTokens.LBRACE ->
                    enclosingBlocks += callsByOpeningBrace[index]

                KtTokens.RBRACE ->
                    if (enclosingBlocks.isNotEmpty()) {
                        enclosingBlocks.removeLast()
                    }

                else -> {
                    val parsedCall =
                        KotlinScriptCallParser.parse(tokens, index)
                            ?: return@forEach

                    parsedCall.openingBraceIndex?.let { braceIndex ->
                        callsByOpeningBrace[braceIndex] = parsedCall.call
                    }

                    val callName =
                        (parsedCall.call.callee as? ScriptCallCallee.Named)?.name

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
