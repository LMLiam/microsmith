package io.github.lmliam.microsmith.compile.services.dotnet.csharp

object CSharp {
    @DslMarker annotation class Dsl

    data class File(val namespace: String, val usings: Set<String>, val types: List<Type>)

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

    enum class Modifier(val keyword: String) {
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

    data class NamedType(val name: String) : TypeRef

    data class GenericType(val name: String, val arguments: List<TypeRef>) : TypeRef

    data class NullableType(val underlying: TypeRef) : TypeRef

    data class ArrayType(val elementType: TypeRef) : TypeRef

    data class TupleType(val elements: List<TupleElement>) : TypeRef

    data class TupleElement(val type: TypeRef, val name: String? = null)

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

    data class Attribute(val name: String, val arguments: List<AttributeArgument> = emptyList())

    sealed interface AttributeArgument

    data class PositionalAttributeArgument(val expression: Expression) : AttributeArgument

    data class NamedAttributeArgument(val name: String, val expression: Expression) : AttributeArgument

    data class Parameter(
        val type: TypeRef,
        val name: String,
        val modifiers: List<Modifier> = emptyList(),
        val attributes: List<Attribute> = emptyList(),
        val defaultValue: String? = null,
    )

    data class CodeBlock(val statements: List<Statement>)

    sealed interface Expression

    data class RawExpression(val text: String) : Expression

    data class StringLiteral(val value: String) : Expression

    data class IntLiteral(val value: Int) : Expression

    data object NullLiteral : Expression

    data class Identifier(val name: String) : Expression

    data class MemberAccess(val target: Expression, val memberName: String) : Expression

    data class Call(val callee: Expression, val arguments: List<CallArgument>) : Expression

    sealed interface CallArgument

    data class ValueCallArgument(val expression: Expression) : CallArgument

    data class OutVariableCallArgument(val name: String) : CallArgument

    data class Await(val expression: Expression) : Expression

    data class Assignment(val target: Expression, val value: Expression) : Expression

    data class IndexAccess(val target: Expression, val arguments: List<Expression>) : Expression

    data class BinaryOperation(val left: Expression, val operator: BinaryOperator, val right: Expression) : Expression

    enum class BinaryOperator(val keyword: String) {
        IS_NOT("is not"),
    }

    data class Conditional(val condition: Expression, val whenTrue: Expression, val whenFalse: Expression) : Expression

    data class ObjectCreation(
        val type: TypeRef,
        val arguments: List<Expression> = emptyList(),
        val initializers: List<MemberInitializer> = emptyList(),
    ) : Expression

    data class MemberInitializer(val memberName: String, val value: Expression)

    data class SwitchExpression(val subject: Expression, val arms: List<SwitchArm>) : Expression

    data class SwitchArm(val pattern: String, val expression: Expression)

    data class Throw(val expression: Expression) : Expression

    data class TupleLiteral(val elements: List<Expression>) : Expression

    sealed interface Statement

    data class RawStatement(val text: String) : Statement

    data class ExpressionStatement(val expression: Expression) : Statement

    data class ReturnStatement(val expression: Expression) : Statement

    data class LocalDeclaration(val keyword: String, val name: String, val initializer: Expression) : Statement

    data class IfStatement(val condition: Expression, val body: CodeBlock) : Statement

    data class StructuredForeachStatement(val target: ForeachTarget, val source: Expression, val body: CodeBlock) :
        Statement

    sealed interface ForeachTarget

    data class ForeachIdentifier(val name: String, val useVarKeyword: Boolean = true) : ForeachTarget

    data class ForeachDeconstruction(val names: List<String>, val useVarKeyword: Boolean = true) : ForeachTarget

    data class RawForeachStatement(val signature: String, val body: CodeBlock) : Statement

    data object BlankLine : Statement
}
