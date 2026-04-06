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
        val modifiers: List<String>,
        val baseTypes: List<String>,
        val attributes: List<Attribute>,
        val primaryConstructorParameters: List<Parameter>,
        val members: List<Member>,
    )

    enum class TypeKind {
        CLASS,
        RECORD,
    }

    sealed interface Member

    data class Property(
        val type: String,
        val name: String,
        val modifiers: List<String>,
        val attributes: List<Attribute>,
        val getter: String,
        val setter: String,
        val initializer: String?,
    ) : Member

    data class Method(
        val name: String,
        val modifiers: List<String>,
        val returnType: String,
        val attributes: List<Attribute>,
        val parameters: List<Parameter>,
        val body: String?,
    ) : Member

    data class Attribute(
        val name: String,
        val arguments: String? = null,
    )

    data class Parameter(
        val type: String,
        val name: String,
        val modifiers: List<String>,
        val attributes: List<Attribute>,
        val defaultValue: String?,
    )

    fun file(namespace: String, build: CSharpFileBuilder.() -> Unit): File =
        CSharpFileBuilder(namespace).apply(build).build()

    fun render(file: File): String = renderCSharp(file)
}
