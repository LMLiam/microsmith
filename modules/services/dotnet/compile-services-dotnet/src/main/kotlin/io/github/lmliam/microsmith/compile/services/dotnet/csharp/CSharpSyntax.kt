package io.github.lmliam.microsmith.compile.services.dotnet.csharp

public fun file(namespace: String, build: CSharpFileBuilder.() -> Unit): CSharp.File =
    CSharpFileBuilder(namespace).apply(build).build()

public fun codeBlock(build: CSharpCodeBlockBuilder.() -> Unit): CSharp.CodeBlock =
    CSharpCodeBlockBuilder().apply(build).build()

public fun rawExpression(text: String): CSharp.Expression = CSharp.RawExpression(text)

public fun stringLiteral(value: String): CSharp.Expression = CSharp.StringLiteral(value)

public fun intLiteral(value: Int): CSharp.Expression = CSharp.IntLiteral(value)

public fun nullLiteral(): CSharp.Expression = CSharp.NullLiteral

public fun identifier(name: String): CSharp.Expression = CSharp.Identifier(name)

public fun member(target: CSharp.Expression, memberName: String): CSharp.Expression =
    CSharp.MemberAccess(target, memberName)

public fun call(callee: CSharp.Expression): CSharp.Expression = CSharp.Call(callee, emptyList())

public fun call(callee: CSharp.Expression, vararg arguments: CSharp.Expression): CSharp.Expression =
    call(
        callee = callee,
        arguments = arguments.map(CSharp::ValueCallArgument),
    )

public fun callValues(callee: CSharp.Expression, arguments: List<CSharp.Expression>): CSharp.Expression =
    call(
        callee = callee,
        arguments = arguments.map(CSharp::ValueCallArgument),
    )

public fun call(callee: CSharp.Expression, vararg arguments: CSharp.CallArgument): CSharp.Expression =
    call(callee, arguments.toList())

public fun call(callee: CSharp.Expression, arguments: List<CSharp.CallArgument>): CSharp.Expression =
    CSharp.Call(callee, arguments)

public fun await(expression: CSharp.Expression): CSharp.Expression = CSharp.Await(expression)

public fun assignment(target: CSharp.Expression, value: CSharp.Expression): CSharp.Expression =
    CSharp.Assignment(target, value)

public fun index(target: CSharp.Expression, vararg arguments: CSharp.Expression): CSharp.Expression =
    CSharp.IndexAccess(
        target = target,
        arguments = arguments.toList(),
    )

public fun binary(
    left: CSharp.Expression,
    operator: CSharp.BinaryOperator,
    right: CSharp.Expression,
): CSharp.Expression =
    CSharp.BinaryOperation(
        left = left,
        operator = operator,
        right = right,
    )

public fun conditional(
    condition: CSharp.Expression,
    whenTrue: CSharp.Expression,
    whenFalse: CSharp.Expression,
): CSharp.Expression = CSharp.Conditional(condition, whenTrue, whenFalse)

public fun new(
    type: CSharp.TypeRef,
    arguments: List<CSharp.Expression> = emptyList(),
    initializers: List<CSharp.MemberInitializer> = emptyList(),
): CSharp.Expression =
    CSharp.ObjectCreation(
        type = type,
        arguments = arguments,
        initializers = initializers,
    )

public fun init(memberName: String, value: CSharp.Expression): CSharp.MemberInitializer =
    CSharp.MemberInitializer(memberName, value)

public fun switch(subject: CSharp.Expression, vararg arms: CSharp.SwitchArm): CSharp.Expression =
    switch(subject, arms.toList())

public fun switch(subject: CSharp.Expression, arms: List<CSharp.SwitchArm>): CSharp.Expression =
    CSharp.SwitchExpression(subject, arms)

public fun switchArm(pattern: String, expression: CSharp.Expression): CSharp.SwitchArm =
    CSharp.SwitchArm(pattern, expression)

public fun throwExpression(expression: CSharp.Expression): CSharp.Expression = CSharp.Throw(expression)

public fun tupleLiteral(vararg elements: CSharp.Expression): CSharp.Expression = CSharp.TupleLiteral(elements.toList())

public fun attribute(name: String, vararg arguments: CSharp.AttributeArgument): CSharp.Attribute =
    CSharp.Attribute(
        name = name,
        arguments = arguments.toList(),
    )

public fun positionalArgument(expression: CSharp.Expression): CSharp.AttributeArgument =
    CSharp.PositionalAttributeArgument(expression)

public fun namedArgument(name: String, expression: CSharp.Expression): CSharp.AttributeArgument =
    CSharp.NamedAttributeArgument(
        name = name,
        expression = expression,
    )

public fun argument(expression: CSharp.Expression): CSharp.CallArgument = CSharp.ValueCallArgument(expression)

public fun outVariable(name: String): CSharp.CallArgument = CSharp.OutVariableCallArgument(name)
