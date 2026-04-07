package io.github.lmliam.microsmith.compile.services.dotnet.csharp

internal fun renderAttribute(attribute: CSharp.Attribute): String =
    attribute.arguments.takeIf(List<CSharp.AttributeArgument>::isNotEmpty)?.let { arguments ->
        "[${attribute.name}(${arguments.joinToString(", ", transform = ::renderAttributeArgument)})]"
    } ?: "[${attribute.name}]"

private fun renderAttributeArgument(argument: CSharp.AttributeArgument): String {
    return when (argument) {
        is CSharp.NamedAttributeArgument ->
            "${argument.name} = ${renderAttributeExpression(argument.expression)}"

        is CSharp.PositionalAttributeArgument ->
            renderAttributeExpression(argument.expression)
    }
}

private fun renderAttributeExpression(expression: CSharp.Expression): String {
    return when (expression) {
        is CSharp.IntLiteral -> expression.value.toString()
        CSharp.NullLiteral -> "null"
        is CSharp.RawExpression -> expression.text
        is CSharp.StringLiteral -> renderStringLiteral(expression.value)
        is CSharp.Assignment,
        is CSharp.Await,
        is CSharp.BinaryOperation,
        is CSharp.Call,
        is CSharp.Conditional,
        is CSharp.Identifier,
        is CSharp.IndexAccess,
        is CSharp.MemberAccess,
        is CSharp.ObjectCreation,
        is CSharp.SwitchExpression,
        is CSharp.Throw,
        is CSharp.TupleLiteral,
        -> error("C# attribute arguments must render from literal-safe expressions.")
    }
}

internal fun indent(text: String, spaces: Int = 4): String {
    val padding = " ".repeat(spaces)
    return text.lineSequence().joinToString("\n") { line ->
        if (line.isEmpty()) {
            line
        } else {
            padding + line
        }
    }
}

internal fun renderStringLiteral(value: String): String {
    return buildString {
        append('"')
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(character)
            }
        }
        append('"')
    }
}
