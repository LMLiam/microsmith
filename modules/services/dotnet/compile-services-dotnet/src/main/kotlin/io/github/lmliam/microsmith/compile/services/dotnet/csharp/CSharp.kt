package io.github.lmliam.microsmith.compile.services.dotnet.csharp

public object CSharp {
    @DslMarker public annotation class Dsl

    public data class File(public val namespace: String, public val usings: Set<String>, public val types: List<Type>)

    public data class Type(
        public val kind: TypeKind,
        public val name: String,
        public val modifiers: List<Modifier>,
        public val baseTypes: List<TypeRef> = emptyList(),
        public val attributes: List<Attribute> = emptyList(),
        public val primaryConstructorParameters: List<Parameter> = emptyList(),
        public val members: List<Member> = emptyList(),
    )

    public enum class TypeKind {
        CLASS,
        RECORD,
    }

    public enum class Modifier(public val keyword: String) {
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

    public enum class PropertyAccessors {
        READ_ONLY,
        READ_WRITE,
        READ_INIT,
    }

    public sealed interface Member

    public sealed interface TypeRef

    public data class NamedType(public val name: String) : TypeRef

    public data class GenericType(public val name: String, public val arguments: List<TypeRef>) : TypeRef

    public data class NullableType(public val underlying: TypeRef) : TypeRef

    public data class ArrayType(public val elementType: TypeRef) : TypeRef

    public data class TupleType(public val elements: List<TupleElement>) : TypeRef

    public data class TupleElement(public val type: TypeRef, public val name: String? = null)

    public data class Property(
        public val type: TypeRef,
        public val name: String,
        public val modifiers: List<Modifier>,
        public val attributes: List<Attribute> = emptyList(),
        public val accessors: PropertyAccessors = PropertyAccessors.READ_WRITE,
        public val initializer: String? = null,
    ) : Member

    public data class Method(
        public val name: String,
        public val modifiers: List<Modifier>,
        public val returnType: TypeRef,
        public val attributes: List<Attribute> = emptyList(),
        public val parameters: List<Parameter> = emptyList(),
        public val body: CodeBlock? = null,
    ) : Member

    public data class Attribute(public val name: String, public val arguments: List<AttributeArgument> = emptyList())

    public sealed interface AttributeArgument

    public data class PositionalAttributeArgument(public val expression: Expression) : AttributeArgument

    public data class NamedAttributeArgument(public val name: String, public val expression: Expression) :
        AttributeArgument

    public data class Parameter(
        public val type: TypeRef,
        public val name: String,
        public val modifiers: List<Modifier> = emptyList(),
        public val attributes: List<Attribute> = emptyList(),
        public val defaultValue: String? = null,
    )

    public data class CodeBlock(public val statements: List<Statement>)

    public sealed interface Expression

    public data class RawExpression(public val text: String) : Expression

    public data class StringLiteral(public val value: String) : Expression

    public data class IntLiteral(public val value: Int) : Expression

    public data object NullLiteral : Expression

    public data class Identifier(public val name: String) : Expression

    public data class MemberAccess(public val target: Expression, public val memberName: String) : Expression

    public data class Call(public val callee: Expression, public val arguments: List<CallArgument>) : Expression

    public sealed interface CallArgument

    public data class ValueCallArgument(public val expression: Expression) : CallArgument

    public data class OutVariableCallArgument(public val name: String) : CallArgument

    public data class Await(public val expression: Expression) : Expression

    public data class Assignment(public val target: Expression, public val value: Expression) : Expression

    public data class IndexAccess(public val target: Expression, public val arguments: List<Expression>) : Expression

    public data class BinaryOperation(
        public val left: Expression,
        public val operator: BinaryOperator,
        public val right: Expression,
    ) : Expression

    public enum class BinaryOperator(public val keyword: String) {
        IS_NOT("is not"),
    }

    public data class Conditional(
        public val condition: Expression,
        public val whenTrue: Expression,
        public val whenFalse: Expression,
    ) : Expression

    public data class ObjectCreation(
        public val type: TypeRef,
        public val arguments: List<Expression> = emptyList(),
        public val initializers: List<MemberInitializer> = emptyList(),
    ) : Expression

    public data class MemberInitializer(public val memberName: String, public val value: Expression)

    public data class SwitchExpression(public val subject: Expression, public val arms: List<SwitchArm>) : Expression

    public data class SwitchArm(public val pattern: String, public val expression: Expression)

    public data class Throw(public val expression: Expression) : Expression

    public data class TupleLiteral(public val elements: List<Expression>) : Expression

    public sealed interface Statement

    public data class RawStatement(public val text: String) : Statement

    public data class ExpressionStatement(public val expression: Expression) : Statement

    public data class ReturnStatement(public val expression: Expression) : Statement

    public data class LocalDeclaration(
        public val keyword: String,
        public val name: String,
        public val initializer: Expression,
    ) : Statement

    public data class IfStatement(public val condition: Expression, public val body: CodeBlock) : Statement

    public data class StructuredForeachStatement(
        public val target: ForeachTarget,
        public val source: Expression,
        public val body: CodeBlock,
    ) : Statement

    public sealed interface ForeachTarget

    public data class ForeachIdentifier(public val name: String, public val useVarKeyword: Boolean = true) :
        ForeachTarget

    public data class ForeachDeconstruction(public val names: List<String>, public val useVarKeyword: Boolean = true) :
        ForeachTarget

    public data class RawForeachStatement(public val signature: String, public val body: CodeBlock) : Statement

    public data object BlankLine : Statement
}
