package io.github.lmliam.microsmith.compile.services.dotnet.csharp

internal fun renderCodeBlock(block: CSharp.CodeBlock): String {
    return indent(block.statements.joinToString("\n", transform = ::renderStatement))
}

private fun renderStatement(statement: CSharp.Statement): String {
    return when (statement) {
        CSharp.BlankLine -> ""
        is CSharp.ExpressionStatement -> "${renderExpression(statement.expression)};"
        is CSharp.ForeachStatement -> buildString {
            appendLine("foreach (${statement.signature})")
            appendLine("{")
            append(renderCodeBlock(statement.body))
            appendLine()
            append("}")
        }
        is CSharp.IfStatement -> buildString {
            appendLine("if (${renderExpression(statement.condition)})")
            appendLine("{")
            append(renderCodeBlock(statement.body))
            appendLine()
            append("}")
        }
        is CSharp.LocalDeclaration ->
            "${statement.keyword} ${statement.name} = ${renderExpression(statement.initializer)};"
        is CSharp.RawStatement -> statement.text
        is CSharp.ReturnStatement -> "return ${renderExpression(statement.expression)};"
    }
}

private fun renderExpression(expression: CSharp.Expression): String {
    return when (expression) {
        is CSharp.Assignment ->
            "${renderExpression(expression.target)} = ${renderExpression(expression.value)}"
        is CSharp.Await -> "await ${renderExpression(expression.expression)}"
        is CSharp.BinaryOperation ->
            "${renderExpression(expression.left)} ${expression.operator} ${renderExpression(expression.right)}"

        is CSharp.Call -> buildString {
            append(renderExpression(expression.callee))
            append("(")
            append(expression.arguments.joinToString(", ", transform = ::renderExpression))
            append(")")
        }
        is CSharp.Conditional -> buildString {
            append(renderExpression(expression.condition))
            appendLine()
            append("? ")
            append(renderExpression(expression.whenTrue))
            appendLine()
            append(": ")
            append(renderExpression(expression.whenFalse))
        }
        is CSharp.Identifier -> expression.name
        is CSharp.IndexAccess -> buildString {
            append(renderExpression(expression.target))
            append("[")
            append(expression.arguments.joinToString(", ", transform = ::renderExpression))
            append("]")
        }

        is CSharp.MemberAccess -> "${renderExpression(expression.target)}.${expression.memberName}"
        is CSharp.ObjectCreation -> buildString {
            append("new ")
            append(renderTypeRef(expression.type))
            append("(")
            append(expression.arguments.joinToString(", ", transform = ::renderExpression))
            append(")")
            if (expression.initializers.isNotEmpty()) {
                appendLine()
                appendLine("{")
                append(
                    indent(
                        expression.initializers.joinToString(",\n") { initializer ->
                            "${initializer.memberName} = ${renderExpression(initializer.value)}"
                        },
                    ),
                )
                appendLine()
                append("}")
            }
        }
        is CSharp.RawExpression -> expression.text
        is CSharp.SwitchExpression -> buildString {
            append(renderExpression(expression.subject))
            appendLine(" switch")
            appendLine("{")
            append(
                indent(
                    expression.arms.joinToString(",\n") { arm ->
                        "${arm.pattern} => ${renderExpression(arm.expression)}"
                    },
                ),
            )
            appendLine()
            append("}")
        }
    }
}
