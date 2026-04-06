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

    sealed interface Statement

    data class RawStatement(
        val text: String,
    ) : Statement

    data class ExpressionStatement(
        val expression: String,
    ) : Statement

    data class ReturnStatement(
        val expression: String,
    ) : Statement

    data class LocalDeclaration(
        val keyword: String,
        val name: String,
        val initializer: String,
    ) : Statement

    data class IfStatement(
        val condition: String,
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

    fun render(file: File): String = renderCSharp(file)
}
