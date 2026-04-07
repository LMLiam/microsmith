package io.github.lmliam.microsmith.compile.services.dotnet.csharp

@Suppress("TooManyFunctions")
object CSharp {
    @DslMarker
    annotation class Dsl

    data class File(
        val namespace: String,
        val usings: Set<String>,
        val types: List<Type>,
    )

    data class Type(
        val kind: TypeKind,
        val name: String,
        val modifiers: List<Modifier>,
        val baseTypes: List<TypeRef> = emptyList(),
        val attributes: List<Attribute> = emptyList(),
        val primaryConstructorParameters: List<Parameter> = emptyList(),
        val members: List<Member> = emptyList(),
    )

    enum class TypeKind {
        CLASS,
        RECORD,
    }

    enum class Modifier(
        val keyword: String,
    ) {
        PUBLIC("public"),
        PRIVATE("private"),
        PROTECTED("protected"),
        ABSTRACT("abstract"),
        STATIC("static"),
        SEALED("sealed"),
        ASYNC("async"),
        THIS("this"),
        PARAMS("params"),
    }

    enum class PropertyAccessors {
        READ_ONLY,
        READ_WRITE,
        READ_INIT,
    }

    sealed interface Member

    sealed interface TypeRef

    data class NamedType(
        val name: String,
    ) : TypeRef

    data class GenericType(
        val name: String,
        val arguments: List<TypeRef>,
    ) : TypeRef

    data class NullableType(
        val underlying: TypeRef,
    ) : TypeRef

    data class ArrayType(
        val elementType: TypeRef,
    ) : TypeRef

    data class TupleType(
        val elements: List<TupleElement>,
    ) : TypeRef

    data class TupleElement(
        val type: TypeRef,
        val name: String? = null,
    )

    data class Property(
        val type: TypeRef,
        val name: String,
        val modifiers: List<Modifier>,
        val attributes: List<Attribute> = emptyList(),
        val accessors: PropertyAccessors = PropertyAccessors.READ_WRITE,
        val initializer: String? = null,
    ) : Member

    data class Method(
        val name: String,
        val modifiers: List<Modifier>,
        val returnType: TypeRef,
        val attributes: List<Attribute> = emptyList(),
        val parameters: List<Parameter> = emptyList(),
        val body: CodeBlock? = null,
    ) : Member

    data class Attribute(
        val name: String,
        val arguments: List<AttributeArgument> = emptyList(),
    )

    sealed interface AttributeArgument

    data class PositionalAttributeArgument(
        val expression: Expression,
    ) : AttributeArgument

    data class NamedAttributeArgument(
        val name: String,
        val expression: Expression,
    ) : AttributeArgument

    data class Parameter(
        val type: TypeRef,
        val name: String,
        val modifiers: List<Modifier> = emptyList(),
        val attributes: List<Attribute> = emptyList(),
        val defaultValue: String? = null,
    )

    data class CodeBlock(
        val statements: List<Statement>,
    )

    sealed interface Expression

    data class RawExpression(
        val text: String,
    ) : Expression

    data class StringLiteral(
        val value: String,
    ) : Expression

    data class IntLiteral(
        val value: Int,
    ) : Expression

    data object NullLiteral : Expression

    data class Identifier(
        val name: String,
    ) : Expression

    data class MemberAccess(
        val target: Expression,
        val memberName: String,
    ) : Expression

    data class Call(
        val callee: Expression,
        val arguments: List<CallArgument>,
    ) : Expression

    sealed interface CallArgument

    data class ValueCallArgument(
        val expression: Expression,
    ) : CallArgument

    data class OutVariableCallArgument(
        val name: String,
    ) : CallArgument

    data class Await(
        val expression: Expression,
    ) : Expression

    data class Assignment(
        val target: Expression,
        val value: Expression,
    ) : Expression

    data class IndexAccess(
        val target: Expression,
        val arguments: List<Expression>,
    ) : Expression

    data class BinaryOperation(
        val left: Expression,
        val operator: BinaryOperator,
        val right: Expression,
    ) : Expression

    enum class BinaryOperator(
        val keyword: String,
    ) {
        IS_NOT("is not"),
    }

    data class Conditional(
        val condition: Expression,
        val whenTrue: Expression,
        val whenFalse: Expression,
    ) : Expression

    data class ObjectCreation(
        val type: TypeRef,
        val arguments: List<Expression> = emptyList(),
        val initializers: List<MemberInitializer> = emptyList(),
    ) : Expression

    data class MemberInitializer(
        val memberName: String,
        val value: Expression,
    )

    data class SwitchExpression(
        val subject: Expression,
        val arms: List<SwitchArm>,
    ) : Expression

    data class SwitchArm(
        val pattern: String,
        val expression: Expression,
    )

    data class Throw(
        val expression: Expression,
    ) : Expression

    data class TupleLiteral(
        val elements: List<Expression>,
    ) : Expression

    sealed interface Statement

    data class RawStatement(
        val text: String,
    ) : Statement

    data class ExpressionStatement(
        val expression: Expression,
    ) : Statement

    data class ReturnStatement(
        val expression: Expression,
    ) : Statement

    data class LocalDeclaration(
        val keyword: String,
        val name: String,
        val initializer: Expression,
    ) : Statement

    data class IfStatement(
        val condition: Expression,
        val body: CodeBlock,
    ) : Statement

    data class StructuredForeachStatement(
        val target: ForeachTarget,
        val source: Expression,
        val body: CodeBlock,
    ) : Statement

    sealed interface ForeachTarget

    data class ForeachIdentifier(
        val name: String,
        val useVarKeyword: Boolean = true,
    ) : ForeachTarget

    data class ForeachDeconstruction(
        val names: List<String>,
        val useVarKeyword: Boolean = true,
    ) : ForeachTarget

    data class RawForeachStatement(
        val signature: String,
        val body: CodeBlock,
    ) : Statement

    data object BlankLine : Statement

    fun file(namespace: String, build: CSharpFileBuilder.() -> Unit): File =
        CSharpFileBuilder(namespace).apply(build).build()

    fun type(name: String): TypeRef = NamedType(name)

    fun genericType(name: String, vararg arguments: TypeRef): TypeRef = GenericType(
        name = name,
        arguments = arguments.toList(),
    )

    fun nullable(type: TypeRef): TypeRef = NullableType(type)

    fun array(type: TypeRef): TypeRef = ArrayType(type)

    fun tuple(vararg elements: TupleElement): TypeRef = TupleType(elements.toList())

    fun tupleElement(type: TypeRef, name: String? = null): TupleElement = TupleElement(type, name)

    fun codeBlock(build: CSharpCodeBlockBuilder.() -> Unit): CodeBlock {
        return CSharpCodeBlockBuilder().apply(build).build()
    }

    fun rawExpression(text: String): Expression = RawExpression(text)

    fun stringLiteral(value: String): Expression = StringLiteral(value)

    fun intLiteral(value: Int): Expression = IntLiteral(value)

    fun nullLiteral(): Expression = NullLiteral

    fun identifier(name: String): Expression = Identifier(name)

    fun member(target: Expression, memberName: String): Expression = MemberAccess(target, memberName)

    fun call(callee: Expression): Expression = Call(callee, emptyList())

    fun call(callee: Expression, vararg arguments: Expression): Expression = call(
        callee = callee,
        arguments = arguments.map(::ValueCallArgument),
    )

    fun callValues(callee: Expression, arguments: List<Expression>): Expression = call(
        callee = callee,
        arguments = arguments.map(::ValueCallArgument),
    )

    fun call(callee: Expression, vararg arguments: CallArgument): Expression = call(callee, arguments.toList())

    fun call(callee: Expression, arguments: List<CallArgument>): Expression = Call(callee, arguments)

    fun await(expression: Expression): Expression = Await(expression)

    fun assignment(target: Expression, value: Expression): Expression = Assignment(target, value)

    fun index(target: Expression, vararg arguments: Expression): Expression = IndexAccess(
        target = target,
        arguments = arguments.toList(),
    )

    fun binary(left: Expression, operator: BinaryOperator, right: Expression): Expression = BinaryOperation(
        left = left,
        operator = operator,
        right = right,
    )

    fun conditional(condition: Expression, whenTrue: Expression, whenFalse: Expression): Expression =
        Conditional(condition, whenTrue, whenFalse)

    fun new(
        type: TypeRef,
        arguments: List<Expression> = emptyList(),
        initializers: List<MemberInitializer> = emptyList(),
    ): Expression = ObjectCreation(
        type = type,
        arguments = arguments,
        initializers = initializers,
    )

    fun init(memberName: String, value: Expression): MemberInitializer = MemberInitializer(memberName, value)

    fun switch(subject: Expression, vararg arms: SwitchArm): Expression = switch(subject, arms.toList())

    fun switch(subject: Expression, arms: List<SwitchArm>): Expression = SwitchExpression(subject, arms)

    fun switchArm(pattern: String, expression: Expression): SwitchArm = SwitchArm(pattern, expression)

    fun throwExpression(expression: Expression): Expression = Throw(expression)

    fun tupleLiteral(vararg elements: Expression): Expression = TupleLiteral(elements.toList())

    fun attribute(name: String, vararg arguments: AttributeArgument): Attribute = Attribute(
        name = name,
        arguments = arguments.toList(),
    )

    fun positionalArgument(expression: Expression): AttributeArgument = PositionalAttributeArgument(expression)

    fun namedArgument(name: String, expression: Expression): AttributeArgument = NamedAttributeArgument(
        name = name,
        expression = expression,
    )

    fun argument(expression: Expression): CallArgument = ValueCallArgument(expression)

    fun outVariable(name: String): CallArgument = OutVariableCallArgument(name)

    fun render(file: File): String = renderCSharp(file)
}
