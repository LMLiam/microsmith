package io.github.lmliam.microsmith.compile.services.dotnet.csharp

@CSharp.Dsl
public class CSharpCodeBlockBuilder internal constructor() {
    private val statements = mutableListOf<CSharp.Statement>()

    public fun line(text: String) {
        statements += CSharp.RawStatement(text)
    }

    public fun expression(expression: CSharp.Expression) {
        statements += CSharp.ExpressionStatement(expression)
    }

    public fun expression(text: String) {
        expression(rawExpression(text))
    }

    public fun returnStatement(expression: CSharp.Expression) {
        statements += CSharp.ReturnStatement(expression)
    }

    public fun returnStatement(expression: String) {
        returnStatement(rawExpression(expression))
    }

    public fun local(keyword: String = "var", name: String, initializer: CSharp.Expression) {
        statements +=
            CSharp.LocalDeclaration(
                keyword = keyword,
                name = name,
                initializer = initializer,
            )
    }

    public fun local(keyword: String = "var", name: String, initializer: String) {
        local(keyword = keyword, name = name, initializer = rawExpression(initializer))
    }

    public fun ifStatement(condition: CSharp.Expression, build: CSharpCodeBlockBuilder.() -> Unit) {
        statements +=
            CSharp.IfStatement(
                condition = condition,
                body = CSharpCodeBlockBuilder().apply(build).build(),
            )
    }

    public fun ifStatement(condition: String, build: CSharpCodeBlockBuilder.() -> Unit) {
        ifStatement(rawExpression(condition), build)
    }

    public fun foreach(signature: String, build: CSharpCodeBlockBuilder.() -> Unit) {
        statements +=
            CSharp.RawForeachStatement(
                signature = signature,
                body = CSharpCodeBlockBuilder().apply(build).build(),
            )
    }

    public fun foreach(name: String, source: CSharp.Expression, build: CSharpCodeBlockBuilder.() -> Unit) {
        statements +=
            CSharp.StructuredForeachStatement(
                target = CSharp.ForeachIdentifier(name),
                source = source,
                body = CSharpCodeBlockBuilder().apply(build).build(),
            )
    }

    public fun foreachDeconstruction(
        vararg names: String,
        source: CSharp.Expression,
        build: CSharpCodeBlockBuilder.() -> Unit,
    ) {
        statements +=
            CSharp.StructuredForeachStatement(
                target = CSharp.ForeachDeconstruction(names.toList()),
                source = source,
                body = CSharpCodeBlockBuilder().apply(build).build(),
            )
    }

    public fun blankLine() {
        statements += CSharp.BlankLine
    }

    internal fun build(): CSharp.CodeBlock = CSharp.CodeBlock(statements)
}
