package io.github.lmliam.microsmith.compile.services.dotnet.csharp

@CSharp.Dsl
class CSharpCodeBlockBuilder internal constructor() {
    private val statements = mutableListOf<CSharp.Statement>()

    fun addStatement(statement: CSharp.Statement) {
        statements += statement
    }

    fun line(text: String) {
        statements += CSharp.RawStatement(text)
    }

    fun expression(text: String) {
        statements += CSharp.ExpressionStatement(text)
    }

    fun returnStatement(expression: String) {
        statements += CSharp.ReturnStatement(expression)
    }

    fun local(keyword: String = "var", name: String, initializer: String) {
        statements += CSharp.LocalDeclaration(
            keyword = keyword,
            name = name,
            initializer = initializer,
        )
    }

    fun ifStatement(condition: String, build: CSharpCodeBlockBuilder.() -> Unit) {
        statements += CSharp.IfStatement(
            condition = condition,
            body = CSharpCodeBlockBuilder().apply(build).build(),
        )
    }

    fun foreach(signature: String, build: CSharpCodeBlockBuilder.() -> Unit) {
        statements += CSharp.ForeachStatement(
            signature = signature,
            body = CSharpCodeBlockBuilder().apply(build).build(),
        )
    }

    fun blankLine() {
        statements += CSharp.BlankLine
    }

    internal fun build(): CSharp.CodeBlock = CSharp.CodeBlock(statements)
}
