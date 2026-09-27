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

    fun expression(expression: CSharp.Expression) {
        statements += CSharp.ExpressionStatement(expression)
    }

    fun expression(text: String) {
        expression(CSharp.rawExpression(text))
    }

    fun returnStatement(expression: CSharp.Expression) {
        statements += CSharp.ReturnStatement(expression)
    }

    fun returnStatement(expression: String) {
        returnStatement(CSharp.rawExpression(expression))
    }

    fun local(keyword: String = "var", name: String, initializer: CSharp.Expression) {
        statements += CSharp.LocalDeclaration(
            keyword = keyword,
            name = name,
            initializer = initializer,
        )
    }

    fun local(keyword: String = "var", name: String, initializer: String) {
        local(keyword = keyword, name = name, initializer = CSharp.rawExpression(initializer))
    }

    fun ifStatement(condition: CSharp.Expression, build: CSharpCodeBlockBuilder.() -> Unit) {
        statements += CSharp.IfStatement(
            condition = condition,
            body = CSharpCodeBlockBuilder().apply(build).build(),
        )
    }

    fun ifStatement(condition: String, build: CSharpCodeBlockBuilder.() -> Unit) {
        ifStatement(CSharp.rawExpression(condition), build)
    }

    fun foreach(signature: String, build: CSharpCodeBlockBuilder.() -> Unit) {
        statements += CSharp.RawForeachStatement(
            signature = signature,
            body = CSharpCodeBlockBuilder().apply(build).build(),
        )
    }

    fun foreach(name: String, source: CSharp.Expression, build: CSharpCodeBlockBuilder.() -> Unit) {
        statements += CSharp.StructuredForeachStatement(
            target = CSharp.ForeachIdentifier(name),
            source = source,
            body = CSharpCodeBlockBuilder().apply(build).build(),
        )
    }

    fun foreachDeconstruction(
        vararg names: String,
        source: CSharp.Expression,
        build: CSharpCodeBlockBuilder.() -> Unit,
    ) {
        statements += CSharp.StructuredForeachStatement(
            target = CSharp.ForeachDeconstruction(names.toList()),
            source = source,
            body = CSharpCodeBlockBuilder().apply(build).build(),
        )
    }

    fun blankLine() {
        statements += CSharp.BlankLine
    }

    internal fun build(): CSharp.CodeBlock = CSharp.CodeBlock(statements)
}
