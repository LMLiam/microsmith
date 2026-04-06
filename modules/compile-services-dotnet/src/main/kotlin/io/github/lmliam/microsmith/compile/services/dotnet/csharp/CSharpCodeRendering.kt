package io.github.lmliam.microsmith.compile.services.dotnet.csharp

internal fun renderCodeBlock(block: CSharp.CodeBlock): String {
    return indent(block.statements.joinToString("\n", transform = ::renderStatement))
}

private fun renderStatement(statement: CSharp.Statement): String {
    return when (statement) {
        CSharp.BlankLine -> ""
        is CSharp.ExpressionStatement -> "${statement.expression};"
        is CSharp.ForeachStatement -> buildString {
            appendLine("foreach (${statement.signature})")
            appendLine("{")
            append(renderCodeBlock(statement.body))
            appendLine()
            append("}")
        }
        is CSharp.IfStatement -> buildString {
            appendLine("if (${statement.condition})")
            appendLine("{")
            append(renderCodeBlock(statement.body))
            appendLine()
            append("}")
        }
        is CSharp.LocalDeclaration -> "${statement.keyword} ${statement.name} = ${statement.initializer};"
        is CSharp.RawStatement -> statement.text
        is CSharp.ReturnStatement -> "return ${statement.expression};"
    }
}
