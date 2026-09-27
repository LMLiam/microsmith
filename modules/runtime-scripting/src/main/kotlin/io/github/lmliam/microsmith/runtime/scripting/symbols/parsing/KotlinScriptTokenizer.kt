package io.github.lmliam.microsmith.runtime.scripting.symbols.parsing

import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtTokens

internal object KotlinScriptTokenizer {
    fun tokenize(sourceText: String): List<KotlinScriptToken> {
        val lexer = KotlinLexer()
        lexer.start(sourceText)

        return buildList {
            while (true) {
                val tokenType = lexer.tokenType ?: break

                if (tokenType !in ignoredTokenTypes) {
                    add(
                        KotlinScriptToken(
                            type = tokenType,
                            text = sourceText.substring(lexer.tokenStart, lexer.tokenEnd),
                        )
                    )
                }

                lexer.advance()
            }
        }
    }

    private val ignoredTokenTypes =
        setOf(
            KtTokens.WHITE_SPACE,
            KtTokens.BLOCK_COMMENT,
            KtTokens.EOL_COMMENT,
            KtTokens.SHEBANG_COMMENT,
            KtTokens.DOC_COMMENT,
        )
}
