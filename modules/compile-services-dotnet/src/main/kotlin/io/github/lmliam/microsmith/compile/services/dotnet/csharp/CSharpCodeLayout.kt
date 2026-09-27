package io.github.lmliam.microsmith.compile.services.dotnet.csharp

internal fun shouldInsertSpacerLine(previous: CSharp.Statement, current: CSharp.Statement): Boolean =
    current !is CSharp.BlankLine &&
        previous.isBlockStatement &&
        previous !is CSharp.RawStatement

private val CSharp.Statement.isBlockStatement: Boolean
    get() = when (this) {
        is CSharp.IfStatement -> true
        is CSharp.RawForeachStatement -> true
        is CSharp.StructuredForeachStatement -> true
        CSharp.BlankLine -> false
        is CSharp.ExpressionStatement -> false
        is CSharp.LocalDeclaration -> false
        is CSharp.RawStatement -> false
        is CSharp.ReturnStatement -> false
    }
