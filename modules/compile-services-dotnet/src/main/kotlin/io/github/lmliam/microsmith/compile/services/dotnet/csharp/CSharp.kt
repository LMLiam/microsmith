package io.github.lmliam.microsmith.compile.services.dotnet.csharp

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
        val baseTypes: List<TypeRef>,
        val attributes: List<Attribute>,
        val primaryConstructorParameters: List<Parameter>,
        val members: List<Member>,
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
        val attributes: List<Attribute>,
        val accessors: PropertyAccessors,
        val initializer: String?,
    ) : Member

    data class Method(
        val name: String,
        val modifiers: List<Modifier>,
        val returnType: TypeRef,
        val attributes: List<Attribute>,
        val parameters: List<Parameter>,
        val body: CodeBlock?,
    ) : Member

    data class Attribute(
        val name: String,
        val arguments: String? = null,
    )

    data class Parameter(
        val type: TypeRef,
        val name: String,
        val modifiers: List<Modifier>,
        val attributes: List<Attribute>,
        val defaultValue: String?,
    )

    data class CodeBlock(
        val statements: List<Statement>,
    )

    sealed interface Expression

    data class RawExpression(
        val text: String,
    ) : Expression

    data class Identifier(
        val name: String,
    ) : Expression

    data class MemberAccess(
        val target: Expression,
        val memberName: String,
    ) : Expression

    data class Call(
        val callee: Expression,
        val arguments: List<Expression>,
    ) : Expression

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
        val operator: String,
        val right: Expression,
    ) : Expression

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

    data class ForeachStatement(
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

    fun identifier(name: String): Expression = Identifier(name)

    fun member(target: Expression, memberName: String): Expression = MemberAccess(target, memberName)

    fun call(callee: Expression, vararg arguments: Expression): Expression = call(callee, arguments.toList())

    fun call(callee: Expression, arguments: List<Expression>): Expression = Call(callee, arguments)

    fun await(expression: Expression): Expression = Await(expression)

    fun assignment(target: Expression, value: Expression): Expression = Assignment(target, value)

    fun index(target: Expression, vararg arguments: Expression): Expression = IndexAccess(
        target = target,
        arguments = arguments.toList(),
    )

    fun binary(left: Expression, operator: String, right: Expression): Expression = BinaryOperation(
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

    fun render(file: File): String = renderCSharp(file)
}
