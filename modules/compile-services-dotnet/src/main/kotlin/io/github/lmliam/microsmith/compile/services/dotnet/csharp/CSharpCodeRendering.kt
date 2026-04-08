package io.github.lmliam.microsmith.compile.services.dotnet.csharp

internal fun renderCodeBlock(block: CSharp.CodeBlock): String = indent(
    buildString {
        block.statements.forEachIndexed { index, statement ->
            if (index > 0) {
                val previous = block.statements[index - 1]
                appendLine()
                if (shouldInsertSpacerLine(previous, statement)) {
                    appendLine()
                }
            }
            append(renderStatement(statement))
        }
    },
)

private fun renderStatement(statement: CSharp.Statement): String = when (statement) {
    CSharp.BlankLine -> ""

    is CSharp.ExpressionStatement -> "${renderExpression(statement.expression)};"

    is CSharp.RawForeachStatement -> buildString {
        appendLine("foreach (${statement.signature})")
        appendLine("{")
        append(renderCodeBlock(statement.body))
        appendLine()
        append("}")
    }

    is CSharp.StructuredForeachStatement -> buildString {
        appendLine("foreach (${renderForeachTarget(statement.target)} in ${renderExpression(statement.source)})")
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

@Suppress("CyclomaticComplexMethod")
private fun renderExpression(expression: CSharp.Expression): String = when (expression) {
    is CSharp.Assignment -> renderAssignment(expression)
    is CSharp.Await -> renderAwait(expression)
    is CSharp.BinaryOperation -> renderBinaryOperation(expression)
    is CSharp.Call -> renderCall(expression)
    is CSharp.Conditional -> renderConditional(expression)
    is CSharp.Identifier -> expression.name
    is CSharp.IntLiteral -> expression.value.toString()
    is CSharp.IndexAccess -> renderIndexAccess(expression)
    is CSharp.MemberAccess -> "${renderExpression(expression.target)}.${expression.memberName}"
    CSharp.NullLiteral -> "null"
    is CSharp.ObjectCreation -> renderObjectCreation(expression)
    is CSharp.RawExpression -> expression.text
    is CSharp.StringLiteral -> renderStringLiteral(expression.value)
    is CSharp.SwitchExpression -> renderSwitchExpression(expression)
    is CSharp.Throw -> "throw ${renderExpression(expression.expression)}"
    is CSharp.TupleLiteral -> renderTupleLiteral(expression)
}

private fun renderAssignment(expression: CSharp.Assignment): String =
    "${renderExpression(expression.target)} = ${renderExpression(expression.value)}"

private fun renderAwait(expression: CSharp.Await): String = "await ${renderExpression(expression.expression)}"

private fun renderBinaryOperation(expression: CSharp.BinaryOperation): String =
    "${renderExpression(expression.left)} ${expression.operator.keyword} ${renderExpression(expression.right)}"

private fun renderCall(expression: CSharp.Call): String = buildString {
    append(renderExpression(expression.callee))
    append("(")
    append(expression.arguments.joinToString(", ", transform = ::renderCallArgument))
    append(")")
}

private fun renderConditional(expression: CSharp.Conditional): String = buildString {
    append(renderExpression(expression.condition))
    appendLine()
    append(indent("? ${renderExpression(expression.whenTrue)}"))
    appendLine()
    append(indent(": ${renderExpression(expression.whenFalse)}"))
}

private fun renderIndexAccess(expression: CSharp.IndexAccess): String = buildString {
    append(renderExpression(expression.target))
    append("[")
    append(expression.arguments.joinToString(", ", transform = ::renderExpression))
    append("]")
}

private fun renderObjectCreation(expression: CSharp.ObjectCreation): String = buildString {
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

private fun renderSwitchExpression(expression: CSharp.SwitchExpression): String = buildString {
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

private fun renderTupleLiteral(expression: CSharp.TupleLiteral): String =
    "(" + expression.elements.joinToString(", ", transform = ::renderExpression) + ")"

private fun renderCallArgument(argument: CSharp.CallArgument): String = when (argument) {
    is CSharp.OutVariableCallArgument -> "out var ${argument.name}"
    is CSharp.ValueCallArgument -> renderExpression(argument.expression)
}

private fun renderForeachTarget(target: CSharp.ForeachTarget): String = when (target) {
    is CSharp.ForeachDeconstruction -> {
        val prefix = if (target.useVarKeyword) "var " else ""
        prefix + "(" + target.names.joinToString(", ") + ")"
    }

    is CSharp.ForeachIdentifier -> {
        val prefix = if (target.useVarKeyword) "var " else ""
        prefix + target.name
    }
}
